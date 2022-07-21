/*
 * Copyright (C) 2022, the @authors. All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3.0 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package areca.app.service.matrix;

import static areca.app.service.matrix.JSEvent.EventType.M_BAD_ENCRYPTED;
import static areca.app.service.matrix.JSEvent.EventType.M_ROOM_ENCRYPTED;
import static areca.app.service.matrix.JSEvent.EventType.M_ROOM_MESSAGE;
import static org.polymap.model2.query.Expressions.and;
import static org.polymap.model2.query.Expressions.anyOf;
import static org.polymap.model2.query.Expressions.eq;
import static org.polymap.model2.query.Expressions.or;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;

import org.polymap.model2.query.Expressions;
import org.polymap.model2.runtime.EntityRuntimeContext.EntityStatus;
import org.polymap.model2.runtime.UnitOfWork;

import areca.app.ArecaApp;
import areca.app.model.Address;
import areca.app.model.Anchor;
import areca.app.model.Contact;
import areca.app.model.IM;
import areca.app.model.MatrixSettings;
import areca.app.model.Message;
import areca.app.model.Message.ContentType;
import areca.app.service.ContactAnchorSynchronizer.ContactAnchorStoreRef;
import areca.app.service.Service;
import areca.app.service.SyncableService;
import areca.app.service.TransportService;
import areca.app.service.TypingEvent;
import areca.common.Assert;
import areca.common.Platform;
import areca.common.ProgressMonitor;
import areca.common.Promise;
import areca.common.Promise.Completable;
import areca.common.base.Opt;
import areca.common.base.Sequence;
import areca.common.event.EventManager;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public class MatrixService
        extends Service
        implements SyncableService<MatrixSettings>, TransportService<MatrixSettings> {

    private static final Log LOG = LogFactory.getLog( MatrixService.class );

    protected MatrixSettings    _settings;

    protected MatrixClient      matrix;

    protected String            clientSyncState;


    @Override
    public String label() {
        return "Matrix";
    }

    /**
     * Override to provide another configured client (for testing).
     */
    protected Promise<MatrixClient> initMatrixClient( MatrixSettings settings ) {
        if (matrix != null) {
            return Promise.completed( matrix );
        }
        else {
            // FIXME check if settings have been changed since last call
            matrix = MatrixClient.create( ArecaApp.proxiedUrl( settings.baseUrl.get() ),
                    settings.accessToken.get(), settings.username.get(), settings.deviceId.get() );

            matrix.initCrypto().then( cryptoResult -> {
                LOG.info( "CRYPTO INIT :)" );
                //MatrixClient.console( cryptoResult );
                matrix.setGlobalErrorOnUnknownDevices( false );
            });

            matrix.startClient( 57000 );
            return matrix.waitForStartup()
                    .onSuccess( __ -> {
                        matrix.uploadKeys().then( ___ -> LOG.info( "Keys uploaded." ) );
                        matrix.exportRoomKeys().then( ___ -> LOG.info( "Room keys exported." ) );
                    })
                    .onError( e -> LOG.warn( "ERROR while starting matrix client.", e ) ); // FIXME UI!
        }
    }

    // Transport ******************************************

    @Override
    public Opt<Transport> newTransport( Address receipient, TransportContext ctx, MatrixSettings settings ) {
        return MatrixAddress.check( receipient )
                .map( address -> new MatrixTransport( address, ctx ) );
    }

    @Override
    public Class<MatrixSettings> transportSettingsType() {
        return MatrixSettings.class;
    }

    /**
     *
     */
    protected class MatrixTransport
            extends TransportService.Transport {

        private MatrixAddress       receipient;

        private TransportContext    ctx;

        public MatrixTransport( MatrixAddress receipient, TransportContext ctx ) {
            this.receipient = receipient;
            this.ctx = ctx;
        }

        @Override
        public Promise<Sent> send( TransportMessage msg ) {
            Assert.notNull( matrix, "No Matrix client initialized." ); // XXX show UI
            var monitor = ctx.newMonitor().beginTask( "Send", 2 ).worked( 1 );

            var result = new Promise.Completable<Sent>();
            var content = JSMessage.create();
            content.setMsgtype( "m.text" );
            content.setBody( msg.text );
            //MatrixClient.console( content );
            matrix.sendEvent( receipient.roomId(), "m.room.message", content, "" )
                    .then( sendResult -> {
                        result.complete( new Sent() );
                        monitor.done();
                    })
                    .catch_( err -> {
                        result.completeWithError( new Exception( "Error while sending message.") );
                        monitor.done();
                    });
            return result;
        }
    }


    // Sync ***********************************************

    @Override
    public Sync newSync( SyncType syncType, SyncContext ctx, MatrixSettings settings ) {
        switch (syncType) {
            case FULL : return new FullSync( ctx, settings );
            case INCREMENTAL : return new FullSync( ctx, settings );
            case BACKGROUND : return new BackgroundSync( ctx, settings );
            default : return null;
        }
    }

    @Override
    public Class<MatrixSettings> syncSettingsType() {
        return MatrixSettings.class;
    }

    /**
     *
     */
    protected class BackgroundSync
            extends SyncBase {

        public BackgroundSync( SyncContext ctx, MatrixSettings settings ) {
            super( ctx, settings );
        }


        @Override
        public void dispose() {
            LOG.info( "Dispose: keeping Matrix client" );
        }


        @Override
        public Promise<?> start() {
            return initMatrixClient( settings ).onSuccess( count -> {
                startListenEvents();
                LOG.info( "Start event handling..." );

                Platform.schedule( 3000, () -> {
                    for (var user : matrix.getUsers()) {
//                        LOG.info( "Profile: %s (%s)", user.displayName(), user.userId() );
//                        matrix.getProfileInfo( user.userId(), "avatar_url" ).then( info -> {
//                            MatrixClient.console( info );
//                            info.ifPresent( url -> {
//                                var http = matrix.mxcUrlToHttp( (String)url, 100, 100, "scale", true );
//                                LOG.info( "Avatar: %s", http );
//                            });
//                        });
                    }
                });
            });
        }


        protected void startListenEvents() {
            // decrypted event/message
            matrix.on( "Event.decrypted", _event -> {
                JSEvent event = _event.cast();
                LOG.info( "Decrypted: %s", event.type() );
                MatrixClient.console( _event );
                var content = event.asContentEvent();

                if (((JSMessage)content.content()).isMsgtype( M_BAD_ENCRYPTED )) { // XXX UI
                    LOG.info( "Decrypt: %s", content.messageContent()
                            .orElseThrow( () -> new RuntimeException( "No content message on BAD ENCRYPTED event." ) )
                            .body().stringValue() );
                }
                else {
                    ArecaApp.current().modelUpdates.schedule( uow -> {
                        return ensureMessageAndAnchors( content, true, uow ).then( msg -> {
                            return uow.submit();
                        });
                    });
                }
            });

            // typing...
            matrix.on("RoomMember.typing", (_event, _member) -> {
                MatrixClient.console( _event );
                MatrixClient.console( _member );
                JSMember member = _member.cast();
                EventManager.instance().publish( new TypingEvent( member.userId(), member.typing() ) );
            });

            // event/message
            matrix.on( "Room.timeline", (_event, _room, _toStartOfTimeline) -> {
                JSEvent event = _event.cast();
                LOG.info( "Event: %s - %s", event.eventId(), event.sender() );

                if (M_ROOM_ENCRYPTED.equals( event.type() )) {
                    // force decrypted messages to decrypt (???)
                    Completable<JSCommon> decrypted = new Completable<>();
                    matrix.decryptEventIfNeeded( event ).then( _decrypted -> {
                        MatrixClient.console( event );
                        decrypted.complete( _decrypted );
                    });
                }

                if (M_ROOM_MESSAGE.equals( event.type() )) {
                    MatrixClient.console( event );
                    ArecaApp.current().modelUpdates.schedule( uow -> {
                        var monitor = ArecaApp.instance().newAsyncOperation();
                        monitor.beginTask( "Matrix event", 2 );
                        monitor.worked( 1 );
                        return ensureMessage( event.asContentEvent(), true, uow ).then( msg -> {
                            return uow.submit();
                        })
                        .onSuccess( __ -> monitor.done() )
                        .onError( __ -> monitor.done() );
                    });
                }
            });
        }
    }


    /**
     * Sync un-encrypted messages.
     */
    protected class FullSync extends SyncBase {

        protected UnitOfWork        uow;

        protected ProgressMonitor   monitor;

        public FullSync( SyncContext ctx, MatrixSettings settings ) {
            super( ctx, settings );
            this.uow = ctx.unitOfWork();
        }


        @Override
        public Promise<?> start() {
            monitor = ctx.monitor();
            return initMatrixClient( settings )
                    .then( __ -> syncRooms() )
                    .reduce( new MutableInt(0), (r,__) -> r.increment() )
                    .then( count -> uow.submit().map( submitted -> count ) )
                    .onSuccess( count -> {
                        LOG.info( "Messages checked/submitted: %s", count );
                    });
        }


        protected Promise<Message> syncRooms() {
            JSRoom[] rooms = matrix.getRooms();
            monitor.beginTask( "Matrix sync", rooms.length * 100 );

            return Promise.serial( rooms.length, (Message)null, i -> {
                var room = rooms[i];
                var timeline = Sequence.of( room.timeline() )
                        .map( tl -> tl.event() )
                        .filter( event -> M_ROOM_MESSAGE.equals( event.type() ) )
                        .toList();

                var submon = monitor.subMonitor( 100 ).beginTask( room.name(), timeline.size() );
                LOG.info( "### Room: %s ##########################################", room.name() );
                return Promise.serial( timeline, (Message)null, event -> {
                    return ensureMessageAndAnchors( event.asContentEvent(), false, uow )
                            .onSuccess( __ -> submon.worked( 1 ) );
                });
            });
        }
    }


    /**
     *
     */
    protected abstract class SyncBase extends Sync {

        protected SyncContext       ctx;

        protected MatrixSettings    settings;


        public SyncBase( SyncContext ctx, MatrixSettings settings ) {
            this.ctx = ctx;
            this.settings = settings;
        }


        protected Promise<Message> ensureMessageAndAnchors( ContentEvent event, boolean unread, UnitOfWork uow ) {
            LOG.info( "Event: eventId=%s, roomId=%s", event.eventId(), event.roomId() );
            //MatrixClient.console( event );
            //return Promise.<Message>absent();

            return ensureMessage( event, unread, uow ).then( msg -> {
                if (msg.status() == EntityStatus.CREATED) {
                    return checkContactAnchor( event.sender(), uow ).then( contactAnchor -> {
                        if (contactAnchor != null) {
                            contactAnchor.messages.add( msg );
                            return Promise.completed( msg );
                        }
                        else {
                            return ensureRoomAnchor( event.roomId(), uow ).map( roomAnchor -> {
                                roomAnchor.messages.add( msg );
                                return msg;
                            });
                        }
                    });
                }
                else {
                    return Promise.completed( msg );
                }
            });
        }


        protected Promise<Message> ensureMessage( ContentEvent event, boolean unread, UnitOfWork uow ) {
            JSMessage content = event.content().cast();
            var storeRef = new MessageStoreRef( settings, event.roomId(), event.eventId() );
            return uow.ensureEntity( Message.class,
                    Expressions.eq( Message.TYPE.storeRef, storeRef.encoded() ),
                    proto -> {
                        proto.setStoreRef( new MessageStoreRef( settings, event.roomId(), event.eventId() ) );
                        proto.fromAddress.set( new MatrixAddress( event.sender(), event.roomId() ).encoded() );
                        proto.content.set( content.body().opt().orElse( "" ) );
                        proto.contentType.set( ContentType.PLAIN );
                        proto.unread.set( unread );
                        proto.date.set( event.date() );
                    });
        }


        protected Promise<Anchor> ensureRoomAnchor( String roomId, UnitOfWork uow ) {
            var room = matrix.getRoom( roomId );
            LOG.debug( "room: %s", room.toString2() );
            var storeRef = new RoomAnchorStoreRef( settings, room );
            return uow.ensureEntity( Anchor.class,
                    Expressions.eq( Anchor.TYPE.storeRef, storeRef.encoded() ),
                    proto -> {
                        proto.name.set( room.name() );
                        proto.setStoreRef( storeRef );
                    })
                    .then( anchor -> {
                        matrix.getJoinedRoomMembers( roomId ).asPromise().onSuccess( members -> {
                            MatrixClient.console( members );
                        });
//                        return anchor.image.get() != null
//                                ? Promise.completed( anchor )
//                                : updateAvatarImage( anchor, userId );
                        return Promise.completed( anchor );
                    });
        }


        protected Promise<Anchor> checkContactAnchor( String userId, UnitOfWork uow ) {
            LOG.info( "Contact: userId=%s", userId );
            var user = matrix.getUser( userId );
            MatrixClient.console( user );
            var parts = StringUtils.split( user.displayName(), ' ' );

            var first = "";
            var last = "";
            var imType = "";

            var search = Expressions.TRUE;

            for (int i = 0; i < parts.length; i++) {
                if (parts[i].startsWith( "(" ) && parts[i].endsWith( ")" )) {
                    imType = parts[i].substring( 1, parts[i].length() - 1 );
                    var name = first + " " + last;
                    search = or( search, anyOf( Contact.TYPE.im, and(
                            eq( IM.TYPE.type, imType.toLowerCase() ),
                            eq( IM.TYPE.name, name ) ) ) );
                }
                else if (i == 0) {
                    first = parts[i];
                    search = eq( Contact.TYPE.firstname, first );
                }
                else {
                    last += parts[i];
                    search = and( search, eq( Contact.TYPE.lastname, last ) );
                }
            }

            search = or( search, anyOf( Contact.TYPE.im, and(
                    eq( IM.TYPE.type, "matrix" ),
                    eq( IM.TYPE.name, userId ) ) ) );

            var finalSearch = search;
            LOG.info( "Search: %s", search );

            return uow.query( Contact.class ).executeCollect().then( contacts -> {
                return Sequence.of( contacts )
                        .filter( it -> finalSearch.evaluate( it ) ) // Contact.im[].type/name is not supported by IDB
                        .first()
                        .map( found -> {
                            LOG.info( "Contact: found!" );
                            return found.anchor.ensure( proto -> {
                                proto.name.set( found.label() );
                                proto.setStoreRef( new ContactAnchorStoreRef( found ) );
                                proto.image.set( found.photo.get() );
                            })
                            .then( anchor -> {
                                return anchor.image.get() != null
                                        ? Promise.completed( anchor )
                                        : updateAvatarImage( anchor, userId );
                            });
                        })
                        .orElse( Promise.completed( (Anchor)null ) );
            });
        }


        protected Promise<Anchor> updateAvatarImage( Anchor anchor, String userId ) {
            LOG.info( "Profile: %s ", userId );
            return matrix.getProfileInfoNoError( userId )
                    .then( info -> {
                        //MatrixClient.console( info );
                        if (!info.errorCode().isUndefined()) {
                            LOG.info( "No profile: error=%s", info.errorCode().stringValue() );
                            return Promise.completed( anchor );
                        }
                        else {
                            var url = matrix.mxcUrlToHttp( info.avatarUrl(), 50, 50, "scale", false );
                            url = url.replace( "?width", "&width" ) // Matrix SDK does not know about HTTP proxy
                                    + "&_encode_=BASE64";           // encoding in HttpForwardServlet
                            LOG.info( "Avatar: %s", url );
                            return Platform.xhr( "GET", url )
                                    .overrideMimeType( "text/plain; charset=x-user-defined" )
                                    .submit()
                                    .map( response -> {
                                        anchor.image.set( response.text() );
                                        return anchor;
                                    });
                        }
                    });
        }

        private final static String BASE64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

        public String encode(String s) {
            String r = "", p = "";
            int c = s.length() % 3;
            if (c > 0) {
                for (; c < 3; c++) {
                    p += "=";
                    s += "\0";
                }
            }
            for (c = 0; c < s.length(); c += 3) {
                if (c > 0 && (c / 3 * 4) % 76 == 0) {
                    r += "\r\n";
                }
                int n = (s.charAt(c) << 16) + (s.charAt(c + 1) << 8) + (s.charAt(c + 2));
                int n1 = (n >> 18) & 63, n2 = (n >> 12) & 63, n3 = (n >> 6) & 63, n4 = n & 63;
                r += "" + BASE64_CHARS.charAt(n1) + BASE64_CHARS.charAt(n2) + BASE64_CHARS.charAt(n3) + BASE64_CHARS.charAt(n4);
            }
            return r.substring(0, r.length() - p.length()) + p;
        }
    }

    /**
     *
     */
    protected static class MatrixAddress
            extends Address {

        public static final String PREFIX = "matrix:";

        public static Opt<MatrixAddress> check( Address check ) {
            Assert.that( !MatrixAddress.class.isInstance( check ) );
            return Opt.of( PREFIX.equals( check.prefix )
                    ? new MatrixAddress( check.content, check.ext )
                    : null );
        }

        public MatrixAddress( String address, String roomId ) {
            Assert.that( address.startsWith( "@" ) );
            this.prefix = PREFIX;
            this.content = Assert.notNull( address );
            this.ext = Assert.notNull( roomId );
        }

        public String roomId() {
            return ext;
        }
    }

    /**
     *
     */
    public static class RoomAnchorStoreRef
            extends MatrixStoreRef {

        /** Decode */
        public RoomAnchorStoreRef() { }

        public RoomAnchorStoreRef( MatrixSettings settings, JSRoom room ) {
            super( settings );
            parts.add( room.roomId() );
        }

        public RoomAnchorStoreRef( MatrixSettings settings, JSEvent event ) {
            super( settings );
            parts.add( event.roomId() );
        }

        @Override
        public String prefix() {
            return super.prefix() + "room";
        }

        public String roomId() {
            return parts.get( 1 );
        }
    }
}
