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
import areca.app.model.Anchor;
import areca.app.model.Message;
import areca.app.service.Service;
import areca.app.service.SyncableService;
import areca.app.service.matrix.MatrixClient.Event;
import areca.app.service.matrix.MatrixClient.Room;
import areca.common.Platform;
import areca.common.Promise;
import areca.common.WaitFor;
import areca.common.base.Opt;
import areca.common.base.Sequence;
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

    protected MatrixClient      matrix;

    protected String            clientSyncState;


    @Override
    public String label() {
        return "Matrix";
    }

    protected Promise<MatrixClient> initMatrixClient() {
        if (matrix != null) {
            return Promise.completed( matrix );
        }
        else {
            //var matrixClient = MatrixClient.create( "https://matrix.org" );
            //var matrixClient = MatrixClient.create( "http?uri=https://matrix.org" );
            var result = MatrixClient.create(
                    "http?uri=https://matrix.fulda.social",
                    "@bolo:fulda.social" );

            result.startClient();

            result.once( "sync", (_state, prevState, res) -> {
                OptString state = _state.cast();
                clientSyncState = state.opt().orElse( null );
                LOG.info( "Client sync: %s - %s", clientSyncState, "PREPARED".equals( clientSyncState ) );
            });
            return new WaitFor<>( () -> "PREPARED".equals( clientSyncState ), () -> matrix = result );
        }
    }


    @Override
    public Sync newSync( SyncContext ctx ) {
        return new MatrixSync( ctx );
    }


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
                        LOG.info( "Submitted: %s", count );
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
            return event.messageContent()
                    .ifPresentMap( content -> {
                        LOG.info( "        content: %s", content.getBody().opt().orElse( "???" ) );
                        var storeRef = "matrix:" + event.eventId();
                        MatrixClient.console( event );
                        return uow.ensureEntity( Message.class,
                                Expressions.eq( Message.TYPE.storeRef, storeRef ),
                                proto -> {
                                    proto.storeRef.set( storeRef );
                                    proto.from.set( event.sender() );
                                    proto.content.set( content.getBody().opt().orElse( "" ) );
                                })
                                .onSuccess( message -> anchor.messages.add( message ) )
                                .map( message -> Opt.of( message ) );
                    })
                    .orElse( Promise.<Message>absent() );
        }


        protected void test() {
//        matrix.on( "Room.timeline", (_event, room, toStartOfTimeline) -> {
//            Event event = (Event)_event;
//            if (event.getType().equals( "m.room.message" )) {
//                LOG.info( "Room.timeline: %s", event.getSender() );
//                LOG.info( "content: %s", event.getContent().isUndefined() );
//
//                event.getContent().opt().ifPresent( self -> {
//                    LOG.info( "content: opt().ifPresent!" );
//                });
//                event.getContent().ifPresent( self -> {
//                    LOG.info( "content: ifPresent!" );
//                    LOG.info( "content msgType: %s", event.getContent().getMsgtype().isUndefined() );
//                    event.getContent().getMsgtype().opt().ifPresent( type -> {
//                        LOG.info( "content: msgType: %s", type );
//                    });
//                });
//                //MatrixClient.console( event.getContent().cast() );
//            }
//        });

        //MatrixClient.console( matrix.whoami() );

        matrix.whoami().then( whoami -> {
            LOG.info( "Whoami: %s - %s", whoami.getUserId(), whoami.isGuest() );
            MatrixClient.console( whoami );
        });

        Platform.schedule( 3000, () -> {
            Room[] rooms = matrix.getRooms();
            LOG.info( "Rooms: %s", rooms.length );
            Sequence.of( rooms ).forEach( room -> {
                LOG.info( "room: %s", room.toString2() );
                Sequence.of( room.timeline() ).forEach( timeline -> {
                    LOG.info( "    timeline: %s", timeline.event().toString2() );
                    timeline.event().messageContent().ifPresent( content -> {
                        LOG.info( "        content: %s", content.getBody().opt().orElse( "???" ) );
                    });
                    //MatrixClient.console( timeline.event().content() );
                });
            });
        });

//        matrixClient.publicRooms( (err,data) -> {
//            LOG.info( "Public rooms: ..." );
//            MatrixClient.console( data );
//        });
        }
    }
}
