/*
 * Copyright (C) 2020, the @authors. All rights reserved.
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
package areca.app;

import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.UnitOfWork;

import areca.app.model.Anchor;
import areca.common.Timer;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public class TestDataBuilder {

    private static final Log log = LogFactory.getLog( TestDataBuilder.class );


    public static void run( EntityRepository repo ) {
        try (
            UnitOfWork uow = repo.newUnitOfWork();
        ){
            Timer timer = Timer.start();
            if (uow.query( Anchor.class ).execute().size() > 0) {
                log.info( "exists already. (" + timer.elapsedHumanReadable() + ")" );
                return;
            }
            for (int i=0; i<50; i++) {
                String name = "Anchor-" + i;
                uow.createEntity( Anchor.class, null, (Anchor proto) -> {
                    proto.name.set( name );
                    return proto;
                });
            }
//            for (Contact contact : uow.query( Contact.class ).execute()) {
//                log.info( "" + contact );
//            }
            uow.commit();
        }
    }
}
