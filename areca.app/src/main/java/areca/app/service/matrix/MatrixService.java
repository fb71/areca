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
import static java.util.Collections.singletonList;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.mutable.MutableInt;

import org.polymap.model2.query.Expressions;
import org.polymap.model2.runtime.UnitOfWork;

import areca.app.ArecaApp;
import areca.app.model.Address;
import areca.app.model.Anchor;
import areca.app.model.MatrixSettings;
import areca.app.model.Message;
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
        implements SyncableService, TransportService {

    private static final Log LOG = LogFactory.getLog( MatrixService.class );

    protected MatrixClient      matrix;  // XXX reload if MatrixSettings are modified

    protected String            clientSyncState;


    @Override
    public String label() {
        return "Matrix";
    }

    /**
     * Override to provide another configured client (for testing).
     */
    protected Promise<MatrixClient> initMatrixClient() {
        if (matrix != null) {  // FIXME race cond!
            return Promise.completed( matrix );
        }
        else {
            return ArecaApp.instance().settings()
                    .then( uow -> uow.query( MatrixSettings.class ).executeCollect() )
                    .then( rs -> {
                        Assert.isEqual( 1, rs.size() );
                        var settings = rs.get( 0 );
                        var result = MatrixClient.create( ArecaApp.proxiedUrl( settings.baseUrl.get() ),
                                settings.accessToken.get(), settings.username.get(), settings.deviceId.get() );

                        result.initCrypto().then( cryptoResult -> {
                            LOG.info( "CRYPTO INIT :)" );
                            MatrixClient.console( cryptoResult );
                            result.setGlobalErrorOnUnknownDevices( false );
                        });

                        result.startClient( 57000 );
                        return result.waitForStartup()
                                .onSuccess( __ -> {
                                    result.uploadKeys().then( ___ -> LOG.info( "Keys uploaded." ) );
                                    result.exportRoomKeys().then( ___ -> LOG.info( "Room keys exported." ) );
                                    matrix = result;
                                })
                                .onError( e -> LOG.warn( "ERROR while starting matrix client.", e ) ); // FIXME UI!
                    });
        }
    }


    protected String anchorStoreRef( String roomId ) {
        return "matrix-room:" + roomId;
    }

    @Override
    public Promise<List<Transport>> newTransport( Address receipient, TransportContext ctx ) {
        var address = MatrixAddress.check( receipient );
        return Promise.completed( address.isPresent()
                ? singletonList( new MatrixTransport( address.get(), ctx ) )
                : Collections.emptyList() );
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


    @Override
    public Promise<Sync> newSync( SyncType syncType, SyncContext ctx ) {
        return ArecaApp.instance().settings()
                .then( uow -> uow.query( MatrixSettings.class ).executeCollect() )
                .map( rs -> {
                    if (rs.size() > 1) {
                        throw new IllegalStateException( "To many MatrixSettings: " + rs.size() );
                    }
                    else if (rs.size() == 1) {
                        switch (syncType) {
                            case FULL : return new FullSync( ctx );
                            case INCREMENT : return null;
                            case BACKGROUND : return new BackgroundSync( ctx );
                            default : return null;
                        }
                    }
                    else {
                        return null;
                    }
                });
    }


    /**
     *
     */
    protected class BackgroundSync
            extends Sync {

        private SyncContext ctx;

        public BackgroundSync( SyncContext ctx ) {
            this.ctx = ctx;
        }

        @Override
        public Promise<?> start() {
            return initMatrixClient()
                    .onSuccess( count -> {
                        startListenEvents();
                        LOG.info( "Start listening..." );
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

                    var monitor = ArecaApp.instance().newAsyncOperation();
                    monitor.beginTask( "Matrix event", 3 );

                    var uow = ctx.uowFactory.supply();

                    // decrypt
                    Completable<JSCommon> decrypted = new Completable<>();
                    matrix.decryptEventIfNeeded( event ).then( _decrypted -> {
                        monitor.worked( 1 );
                        MatrixClient.console( _decrypted );
                        MatrixClient.console( event.content() );
                        decrypted.complete( _decrypted );
                    });
                    decrypted
                            // ensure room Anchor
                            .then( __ -> {
                                return uow.ensureEntity( Anchor.class,
                                        Expressions.eq( Anchor.TYPE.storeRef, anchorStoreRef( event.roomId() ) ),
                                        proto -> {
                                            proto.name.set( "New: " + event.sender() );
                                            proto.storeRef.set( anchorStoreRef( event.roomId() ) );
                                        });
                            })
                            // create message
                            .map( anchor -> {
                                monitor.worked( 1 );
                                JSMessage content = event.content().cast();
                                return uow.createEntity( Message.class, proto -> {
                                    MessageStoreRef storeRef = MessageStoreRef.of( event.roomId(), event.eventId() );
                                    proto.storeRef.set( storeRef.toString() );
                                    proto.fromAddress.set( new MatrixAddress( event.sender(), event.roomId() ).encoded() );
                                    proto.content.set( content.getBody().opt().orElse( "" ) );
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
                            .onError( ArecaApp.instance().defaultErrorHandler() )
                            .onError( e -> monitor.done() );

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

        public FullSync( SyncContext ctx ) {
            this.ctx = ctx;
            this.uow = ctx.uowFactory.supply();
        }


        @Override
        public Promise<?> start() {
            return initMatrixClient()
                    .then( __ -> syncRooms() )
                    .reduce( new MutableInt(0), (r,__) -> r.increment() )
                    .then( count -> uow.submit().map( submitted -> count ) )
                    .onSuccess( count -> {
                        LOG.info( "Messages checked/submitted: %s", count );
                    });
        }


        protected Promise<Opt<Message>> syncRooms() {
            JSRoom[] rooms = matrix.getRooms();
            LOG.info( "Rooms: %s", rooms.length );

            // ensure room Anchor
            return Promise.serial( rooms.length, i -> {
                LOG.info( "room: %s", rooms[i].toString2() );
                return ensureRoomAnchor( rooms[i] );
            })
            // timeline/event -> Message
            .then( roomAnchor -> {
                var timeline = roomAnchor.room.timeline();
                return Promise.joined( timeline.length, i -> ensureMessage( roomAnchor, timeline[i].event() ) );
            });
        }


        protected Promise<RoomAnchor> ensureRoomAnchor( JSRoom room ) {
            LOG.info( "room: %s", room.toString2() );
            var storeRef = anchorStoreRef( room.roomId() );
            return uow.ensureEntity( Anchor.class,
                    Expressions.eq( Anchor.TYPE.storeRef, storeRef ),
                    proto -> {
                        proto.name.set( room.name() );
                        proto.storeRef.set( storeRef );
                    })
                    .map( anchor -> RoomAnchor.of( room, anchor ) );
        }


        protected Promise<Opt<Message>> ensureMessage( RoomAnchor roomAnchor, JSStoredEvent event ) {
            LOG.info( "    timeline: %s", event.toString2() );
            // encrypted
            event.encryptedContent().ifPresent( encrypted -> {
                //MatrixClient.console( event );
                matrix.decryptEventIfNeeded( event ).then( decrypted -> {
                    LOG.info( "Decrypted:" );
                    MatrixClient.console( decrypted );
                });
            });
            // plain
            MatrixClient.console( event );
            return event.messageContent()
                    .ifPresentMap( content -> {
                        LOG.info( "        content: %s", content.getBody().opt().orElse( "???" ) );
                        var storeRef = MessageStoreRef.of( roomAnchor.room.roomId(), event.eventId() );
                        return uow.ensureEntity( Message.class,
                                Expressions.eq( Message.TYPE.storeRef, storeRef.toString() ),
                                proto -> {
                                    proto.storeRef.set( storeRef.toString() );
                                    proto.fromAddress.set( new MatrixAddress( event.sender(), roomAnchor.room.roomId() ).encoded() );
                                    proto.content.set( content.getBody().opt().orElse( "" ) );
                                    proto.unread.set( true );
                                    proto.date.set( (long)event.date() );
                                    roomAnchor.anchor.messages.add( proto );
                                })
                                .map( message -> Opt.of( message ) );
                    })
                    .orElse( Promise.<Message>absent() );
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
    protected static class RoomAnchor {
        public JSRoom   room;
        public Anchor   anchor;

        public static RoomAnchor of( JSRoom _room, Anchor _anchor ) {
            return new RoomAnchor() {{ room = _room; anchor = _anchor;}};
        }
    }
}
