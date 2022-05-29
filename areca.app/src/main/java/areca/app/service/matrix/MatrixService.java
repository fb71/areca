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
import org.apache.commons.lang3.tuple.MutablePair;
import org.polymap.model2.query.Expressions;
import org.polymap.model2.runtime.UnitOfWork;

import areca.app.ArecaApp;
import areca.app.model.Anchor;
import areca.app.model.MatrixSettings;
import areca.app.model.Message;
import areca.app.service.Service;
import areca.app.service.SyncableService;
import areca.app.service.matrix.MatrixClient.Event;
import areca.app.service.matrix.MatrixClient.Room;
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
        implements SyncableService {

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

                        result.startClient();
                        return result.waitForStartup()
                                .onSuccess( __ -> matrix = result )
                                .onError( e -> LOG.warn( "ERROR while starting matrix client.", e ) ); // FIXME UI!
                    });
        }
    }


    @Override
    public Promise<Sync> newSync( SyncContext ctx ) {
        return ArecaApp.instance().settings()
                .then( uow -> uow.query( MatrixSettings.class ).executeCollect() )
                .map( rs -> {
                    if (rs.size() > 1) {
                        throw new IllegalStateException( "To many MatrixSettings: " + rs.size() );
                    }
                    else if (rs.size() == 1) {
                        return new MatrixSync( ctx );
                    }
                    else {
                        return null;
                    }
                });
    }


    /**
     *
     */
    protected class MatrixSync
            extends Sync {

        protected SyncContext       ctx;

        private UnitOfWork          uow;

        public MatrixSync( SyncContext ctx ) {
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
            Room[] rooms = matrix.getRooms();
            LOG.info( "Rooms: %s", rooms.length );

            // ensure room Anchor
            return Promise.serial( rooms.length, i -> {
                return ensureRoomAnchor( rooms[i] );
            })
            // timeline/event -> Message
            .then( roomAnchor -> {
                var timeline = roomAnchor.left.timeline();
                return Promise.joined( timeline.length, i -> ensureMessage( roomAnchor.right, timeline[i].event() ) );
            });
        }


        protected Promise<MutablePair<Room,Anchor>> ensureRoomAnchor( Room room ) {
            LOG.info( "room: %s", room.toString2() );
            var storeRef = "matrix-room:" + room.roomId();
            return uow.ensureEntity( Anchor.class,
                    Expressions.eq( Anchor.TYPE.storeRef, storeRef ),
                    proto -> {
                        proto.name.set( room.name() );
                        proto.storeRef.set( storeRef );
                    })
                    .map( anchor -> MutablePair.of( room, anchor ) );
        }


        protected Promise<Opt<Message>> ensureMessage( Anchor anchor, Event event ) {
            LOG.info( "    timeline: %s", event.toString2() );
            // encrypted
            event.encryptedContent().ifPresent( encrypted -> {
                MatrixClient.console( event );
                matrix.decryptEventIfNeeded( event ).then( decrypted -> {
                    MatrixClient.console( decrypted );
                });
            });
            // plain
            return event.messageContent()
                    .ifPresentMap( content -> {
                        LOG.info( "        content: %s", content.getBody().opt().orElse( "???" ) );
                        var storeRef = "matrix:" + event.eventId();
                        return uow.ensureEntity( Message.class,
                                Expressions.eq( Message.TYPE.storeRef, storeRef ),
                                proto -> {
                                    proto.storeRef.set( storeRef );
                                    proto.from.set( event.sender() );
                                    proto.content.set( content.getBody().opt().orElse( "" ) );
                                    proto.unread.set( true );
                                    anchor.messages.add( proto );
                                })
                                .map( message -> Opt.of( message ) );
                    })
                    .orElse( Promise.<Message>absent() );
        }
    }
}
