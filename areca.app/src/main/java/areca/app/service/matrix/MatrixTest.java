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

import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.store.tidbstore.IDBStore;

import areca.app.model.Anchor;
import areca.app.model.Contact;
import areca.app.model.Message;
import areca.app.service.SyncableService;
import areca.common.NullProgressMonitor;
import areca.common.Promise;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
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
    public Promise<?> syncRoomsTest() {
        var service = new MatrixService();
        return initRepo( "syncRooms" )
                .map( repo -> {
                    var ctx = new SyncableService.SyncContext() {{
                        monitor = new NullProgressMonitor();
                        uowFactory = () -> repo.newUnitOfWork();
                    }};
                    return service.newSync( ctx );
                })
                .then( sync -> sync.start() );
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
}
