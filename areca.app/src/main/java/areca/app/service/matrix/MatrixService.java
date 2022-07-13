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

import static areca.app.service.matrix.JSEvent.EventType.M_ROOM_ENCRYPTED;
import static areca.app.service.matrix.JSEvent.EventType.M_ROOM_MESSAGE;
import org.apache.commons.lang3.mutable.MutableInt;

import org.polymap.model2.query.Expressions;
import org.polymap.model2.runtime.UnitOfWork;

import areca.app.ArecaApp;
import areca.app.model.Address;
import areca.app.model.Anchor;
import areca.app.model.MatrixSettings;
import areca.app.model.Message;
import areca.app.model.Message.ContentType;
import areca.app.service.Service;
import areca.app.service.SyncableService;
import areca.app.service.TransportService;
import areca.app.service.TypingEvent;
import areca.common.Assert;
import areca.common.Promise;
import areca.common.Promise.Completable;
import areca.common.base.Opt;
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
            // FIXME check if settings are still...
            matrix = MatrixClient.create( ArecaApp.proxiedUrl( settings.baseUrl.get() ),
                    settings.accessToken.get(), settings.username.get(), settings.deviceId.get() );

            matrix.initCrypto().then( cryptoResult -> {
                LOG.info( "CRYPTO INIT :)" );
                MatrixClient.console( cryptoResult );
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
            extends Sync {

        private SyncContext ctx;

        private MatrixSettings settings;

        public BackgroundSync( SyncContext ctx, MatrixSettings settings ) {
            this.ctx = ctx;
            this.settings = settings;
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
            });
        }


        protected void startListenEvents() {
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

                if (M_ROOM_MESSAGE.equals( event.type() ) || M_ROOM_ENCRYPTED.equals( event.type() )) {
                    MatrixClient.console( event );
                    ArecaApp.current().modelUpdates.schedule( uow -> {
                        var monitor = ArecaApp.instance().newAsyncOperation();
                        monitor.beginTask( "Matrix event", 3 );

                        // decrypt
                        Completable<JSCommon> decrypted = new Completable<>();
                        matrix.decryptEventIfNeeded( event ).then( _decrypted -> {
                            monitor.worked( 1 );
                            MatrixClient.console( _decrypted );
                            MatrixClient.console( event.content() );
                            decrypted.complete( _decrypted );
                        });
                        return decrypted
                                // ensure room Anchor
                                .then( __ -> {
                                    var storeRef = new RoomAnchorStoreRef( settings, event );
                                    return uow.ensureEntity( Anchor.class,
                                            Expressions.eq( Anchor.TYPE.storeRef, storeRef.encoded() ),
                                            proto -> {
                                                proto.name.set( "New: " + event.sender() );
                                                proto.setStoreRef( storeRef );
                                            });
                                })
                                // create message
                                .map( anchor -> {
                                    monitor.worked( 1 );
                                    JSMessage content = event.content().cast();
                                    return uow.createEntity( Message.class, proto -> {
                                        proto.setStoreRef( new MessageStoreRef( settings, event ) );
                                        proto.fromAddress.set( new MatrixAddress( event.sender(), event.roomId() ).encoded() );
                                        proto.content.set( content.getBody().opt().orElse( "" ) );
                                        proto.contentType.set( ContentType.PLAIN );
                                        proto.unread.set( true );
                                        proto.date.set( (long)event.date().getTime() );
                                        anchor.messages.add( proto );
                                    });
                                })
                                // submit
                                .then( message -> {
                                    monitor.worked( 1 );
                                    return uow.submit();
                                })
                                .onSuccess( submitted -> monitor.done() )
                                .onError( e -> monitor.done() );
                    });
                }
            });
        }
    }


    /**
     *
     */
    protected class FullSync
            extends Sync {

        protected SyncContext       ctx;

        private UnitOfWork          uow;

        private MatrixSettings      settings;

        public FullSync( SyncContext ctx, MatrixSettings settings ) {
            this.ctx = ctx;
            this.uow = ctx.unitOfWork();
            this.settings = settings;
        }


        @Override
        public Promise<?> start() {
            return initMatrixClient( settings )
                    .then( __ -> syncRooms() )
                    .reduce( new MutableInt(0), (r,__) -> r.increment() )
                    .then( count -> uow.submit().map( submitted -> count ) )
                    .onSuccess( count -> {
                        LOG.info( "Messages checked/submitted: %s", count );
                    });
        }


        protected Promise<Opt<Message>> syncRooms() {
            JSRoom[] rooms = matrix.getRooms();
            LOG.debug( "Rooms: %s", rooms.length );

            // ensure room Anchor
            return Promise.serial( rooms.length, i -> {
                LOG.debug( "room: %s", rooms[i].toString2() );
                return ensureRoomAnchor( rooms[i] );
            })
            // timeline/event -> Message
            .then( roomAnchor -> {
                var timeline = roomAnchor.room.timeline();
                return Promise.joined( timeline.length, i -> ensureMessage( roomAnchor, timeline[i].event() ) );
            });
        }


        protected Promise<RoomAndAnchor> ensureRoomAnchor( JSRoom room ) {
            LOG.debug( "room: %s", room.toString2() );
            var storeRef = new RoomAnchorStoreRef( settings, room );
            return uow.ensureEntity( Anchor.class,
                    Expressions.eq( Anchor.TYPE.storeRef, storeRef.encoded() ),
                    proto -> {
                        proto.name.set( room.name() );
                        proto.setStoreRef( storeRef );
                    })
                    .map( anchor -> new RoomAndAnchor( room, anchor ) );
        }


        protected Promise<Opt<Message>> ensureMessage( RoomAndAnchor roomAnchor, JSStoredEvent event ) {
            LOG.debug( "    timeline: %s", event.toString2() );
            // encrypted
            event.encryptedContent().ifPresent( encrypted -> {
                //MatrixClient.console( event );
                matrix.decryptEventIfNeeded( event ).then( decrypted -> {
                    LOG.debug( "Decrypted:" );
                    //MatrixClient.console( decrypted );
                });
            });
            // plain
            MatrixClient.console( event );
            return event.messageContent()
                    .ifPresentMap( content -> {
                        LOG.debug( "        content: %s", content.getBody().opt().orElse( "???" ) );
                        var storeRef = new MessageStoreRef( settings, roomAnchor.room.roomId(), event.eventId() );
                        return uow.ensureEntity( Message.class,
                                Expressions.eq( Message.TYPE.storeRef, storeRef.encoded() ),
                                proto -> {
                                    proto.setStoreRef( storeRef );
                                    proto.fromAddress.set( new MatrixAddress( event.sender(), roomAnchor.room.roomId() ).encoded() );
                                    proto.content.set( content.getBody().opt().orElse( "" ) );
                                    proto.contentType.set( ContentType.PLAIN );
                                    proto.unread.set( false );
                                    proto.date.set( (long)event.date() );
                                    roomAnchor.anchor.messages.add( proto );
                                })
                                .map( message -> Opt.of( message ) );
                    })
                    .orElse( Promise.<Message>absent() );
        }

        /**
         *
         */
        protected class RoomAndAnchor {
            public JSRoom   room;
            public Anchor   anchor;

            public RoomAndAnchor( JSRoom _room, Anchor _anchor ) {
                this.room = _room;
                this.anchor = _anchor;
            }
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
