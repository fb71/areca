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

import org.apache.commons.lang3.mutable.MutableInt;
import org.polymap.model2.query.Expressions;
import org.polymap.model2.runtime.UnitOfWork;

import areca.app.ArecaApp;
import areca.app.model.Anchor;
import areca.app.model.MatrixSettings;
import areca.app.model.Message;
import areca.app.service.Service;
import areca.app.service.SyncableService;
import areca.app.service.TransportService;
import areca.common.Assert;
import areca.common.Promise;
import areca.common.base.Opt;
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
        if (matrix != null) {
            return Promise.completed( matrix );
        }
        else {
            return ArecaApp.instance().settings()
                    .then( uow -> uow.query( MatrixSettings.class ).executeCollect() )
                    .then( rs -> {
                        Assert.isEqual( 1, rs.size() );
                        var settings = rs.get( 0 );
                        var result = MatrixClient.create( ArecaApp.proxiedUrl( settings.baseUrl.get() ),
                                settings.accessToken.get(), settings.username.get() );

//                        result.initCrypto().then( cryptoResult -> {
//                            LOG.info( "CRYPTO INIT :)" );
//                            MatrixClient.console( cryptoResult );
//                            result.setGlobalErrorOnUnknownDevices( false );
//                        });

                        result.startClient( 58000 );
                        return result.waitForStartup()
                                .onSuccess( __ -> matrix = result )
                                .onError( e -> LOG.warn( "ERROR while starting matrix client.", e ) ); // FIXME UI!
                    });
        }
    }


    protected String anchorStoreRef( String roomId ) {
        return "matrix-room:" + roomId;
    }

    @Override
    public Promise<TransportService.Transport> newTransport( String receipient, TransportContext ctx ) {
        var storeRef = MessageStoreRef.parse( receipient );
        return storeRef.isPresent()
                ? Promise.completed( new Transport( storeRef.get(), ctx ) )
                : Promise.completed( null );
    }


    /**
     *
     */
    protected class Transport
            extends TransportService.Transport {

        private MessageStoreRef     receipient;

        private TransportContext    ctx;

        public Transport( MessageStoreRef receipient, TransportContext ctx ) {
            this.receipient = receipient;
            this.ctx = ctx;
        }

        @Override
        public Promise<Sent> send( String text ) {
            Assert.notNull( matrix, "No Matrix client initialized." ); // XXX show UI
            var result = new Promise.Completable<Sent>();
            var content = JSMessage.create();
            content.setMsgtype( "m.text" );
            content.setBody( text );
            MatrixClient.console( content );
            matrix.sendEvent( receipient.roomId, "m.room.message", content, "" )
                    .then( sendResult -> result.complete( new Sent() ) );
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
                            case BACKGROUND : return new PermanentSync( ctx );
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
    protected class PermanentSync
            extends Sync {

        private SyncContext ctx;

        public PermanentSync( SyncContext ctx ) {
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
            matrix.on( "Room.timeline", (_event, _room, _toStartOfTimeline) -> {
                JSEvent event = _event.cast();
                LOG.info( "Event: %s - %s", event.eventId(), event.sender() );
                MatrixClient.console( _event );

                event.messageContent().ifPresent( content -> {
                    LOG.info( "    Content: %s", content.getBody().opt().orElse( "???" ) );

                    var monitor = ArecaApp.instance().newAsyncOperation();
                    monitor.beginTask( "Matrix event", 3 );
                    monitor.worked( 1 );

                    var uow = ctx.uowFactory.supply();
                    uow
                            // ensure room Anchor
                            .ensureEntity( Anchor.class,
                                    Expressions.eq( Anchor.TYPE.storeRef, anchorStoreRef( event.roomId() ) ),
                                    proto -> {
                                        proto.name.set( "New: " + event.sender() );
                                        proto.storeRef.set( anchorStoreRef( event.roomId() ) );
                                    })
                            // create message
                            .map( anchor -> {
                                monitor.worked( 1 );
                                return uow.createEntity( Message.class, proto -> {
                                    MessageStoreRef storeRef = MessageStoreRef.of( event.roomId(), event.eventId() );
                                    proto.storeRef.set( storeRef.toString() );
                                    proto.from.set( storeRef.toString() );
                                    proto.from.set( event.sender() );
                                    proto.content.set( content.getBody().opt().orElse( "" ) );
                                    proto.unread.set( true );
                                    proto.date.set( (long)event.date().getTime() );
                                    anchor.messages.add( proto );
                                });
                            })
                            // submit
                            .then( message -> {
                                return uow.submit();
                            })
                            .onSuccess( submitted -> monitor.done() )
                            .onError( ArecaApp.instance().defaultErrorHandler() )
                            .onError( e -> monitor.done() );

                });
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
                                    proto.from.set( storeRef.toString() );
                                    //proto.from.set( event.sender() );
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
    protected static class RoomAnchor {
        public JSRoom   room;
        public Anchor   anchor;

        public static RoomAnchor of( JSRoom _room, Anchor _anchor ) {
            return new RoomAnchor() {{ room = _room; anchor = _anchor;}};
        }
    }
}
