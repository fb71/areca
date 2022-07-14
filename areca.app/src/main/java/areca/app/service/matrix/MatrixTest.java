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

import java.util.Arrays;

import org.teavm.jso.core.JSString;

import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.store.tidbstore.IDBStore;

import areca.app.model.Anchor;
import areca.app.model.Contact;
import areca.app.model.Message;
import areca.app.model.ModelUpdateEvent;
import areca.app.service.SyncableService;
import areca.app.service.SyncableService.SyncType;
import areca.common.NullProgressMonitor;
import areca.common.ProgressMonitor;
import areca.common.Promise;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.common.testrunner.Skip;
import areca.common.testrunner.Test;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class MatrixTest {

    private static final Log LOG = LogFactory.getLog( MatrixTest.class );

    public static final ClassInfo<MatrixTest> info = MatrixTestClassInfo.instance();


    protected Promise<EntityRepository> initRepo( String name ) {
        return EntityRepository.newConfiguration()
                .entities.set( Arrays.asList( Message.info, Contact.info, Anchor.info) )
                .store.set( new IDBStore( "MatrixTest-" + name, IDBStore.nextDbVersion(), true ) )
                .create();
    }


    @Test
    @Skip
    public void loginTest() {
        var matrix = MatrixClient.create( "https://matrix.fulda.social" );
        var p = matrix.loginWithPassword( "@bolo:fulda.social", "1Bolonia8" );
        MatrixClient.console( p );
        p.then( credentials -> {
            MatrixClient.console( credentials );
        }).catch_( err -> {
            String s = ((JSString)err).stringValue();
            LOG.info( "Error: %s", s );
            MatrixClient.console( err );
        });
    }

    @Test
    public Promise<?> verifyTest() {
        String userId = "@bolo:fulda.social";
        var matrix = MatrixClient.create( "https://matrix.fulda.social",
                "syt_Ym9sbw_eXvFQbDTVWfzSONFpJJv_4SO6zS", userId, "QRWKUGRCWK" );

        matrix.initCrypto().then( cryptoResult -> {
            LOG.info( "CRYPTO INIT :)" );
            MatrixClient.console( cryptoResult );
            matrix.setGlobalErrorOnUnknownDevices( false );
        });

        matrix.startClient( 58000 );
        return matrix.waitForStartup()
                .onSuccess( __ -> {
                    var devices = matrix.getStoredDevicesForUser( "@bolo:fulda.social" );
                    for (int i=0; i<devices.length; i++) {
                        MatrixClient.console( devices[i] );
                        matrix.setDeviceKnown( userId, devices[i].deviceId(), true );
                        matrix.setDeviceVerified( userId, devices[i].deviceId(), true );
                    }
                })
                .onError( e -> LOG.warn( "ERROR while starting matrix client.", e ) );
    }


    @Test
    @Skip
    public Promise<?> syncServiceTest() {
        var service = new MatrixService() {
//            @Override
//            protected Promise<MatrixClient> initMatrixClient() {
//                throw new RuntimeException( "not yet implemented." );
//            }
        };
        return initRepo( "syncService" )
                .then( repo -> {
                    var ctx = new SyncableService.SyncContext() {
                        @Override public ProgressMonitor monitor() {
                            return new NullProgressMonitor();
                        }
                        @Override public UnitOfWork unitOfWork() {
                            return repo.newUnitOfWork();
                        }
                        @Override public ModelUpdateEvent outgoing() {
                            throw new RuntimeException( "do not call" );
                        }
                    };
                    return service.newSync( SyncType.FULL, ctx, null ).start();
                })
                .onSuccess( __ -> {
                    //Platform.schedule( 3000, () -> service.matrix.stopClient() );
                });
    }


//    @Test
//    public Promise<?> syncRoomsTest() {
//        var service = new MatrixService();
//        return service.initMatrixClient()
//                .then( __ -> initRepo( "syncRooms" ) )
//                .map( repo -> {
//                    return new SyncableService.SyncContext() {{
//                        monitor = new NullProgressMonitor();
//                        uowFactory = () -> repo.newUnitOfWork();
//                    }};
//                })
//                .then( ctx -> ((MatrixService.MatrixSync)service.newSync( ctx )).start )
//    }


    protected void roomTimelineTest() {
//        MatrixClient matrix = null;
//
//      matrix.on( "Room.timeline", (_event, room, toStartOfTimeline) -> {
//          Event event = (Event)_event;
//          if (event.getType().equals( "m.room.message" )) {
//              LOG.info( "Room.timeline: %s", event.getSender() );
//              LOG.info( "content: %s", event.getContent().isUndefined() );
//
//              event.getContent().opt().ifPresent( self -> {
//                  LOG.info( "content: opt().ifPresent!" );
//              });
//              event.getContent().ifPresent( self -> {
//                  LOG.info( "content: ifPresent!" );
//                  LOG.info( "content msgType: %s", event.getContent().getMsgtype().isUndefined() );
//                  event.getContent().getMsgtype().opt().ifPresent( type -> {
//                      LOG.info( "content: msgType: %s", type );
//                  });
//              });
//              //MatrixClient.console( event.getContent().cast() );
//          }
//      });

      //MatrixClient.console( matrix.whoami() );

    }


//    protected void whoamiTest() {
//        matrix.whoami().then( whoami -> {
//            LOG.info( "Whoami: %s - %s", whoami.getUserId(), whoami.isGuest() );
//            MatrixClient.console( whoami );
//        });
//    }
//
//      Platform.schedule( 3000, () -> {
//          Room[] rooms = matrix.getRooms();
//          LOG.info( "Rooms: %s", rooms.length );
//          Sequence.of( rooms ).forEach( room -> {
//              LOG.info( "room: %s", room.toString2() );
//              Sequence.of( room.timeline() ).forEach( timeline -> {
//                  LOG.info( "    timeline: %s", timeline.event().toString2() );
//                  timeline.event().messageContent().ifPresent( content -> {
//                      LOG.info( "        content: %s", content.getBody().opt().orElse( "???" ) );
//                  });
//                  //MatrixClient.console( timeline.event().content() );
//              });
//          });
//      });
//
//      }

}
