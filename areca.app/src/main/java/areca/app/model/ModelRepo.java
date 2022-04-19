/*
 * Copyright (C) 2021, the @authors. All rights reserved.
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
package areca.app.model;

import static java.util.Arrays.asList;

import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.store.tidbstore.IDBStore;

import areca.common.Assert;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public class ModelRepo {

    private static final Log LOG = LogFactory.getLog( ModelRepo.class );

    private static EntityRepository instance;

    private static UnitOfWork uow;


    public static void init() {
        Assert.isNull( instance );
        EntityRepository.newConfiguration()
                .entities.set( asList( Message.info, Contact.info, Anchor.info) )
                .store.set( new IDBStore( "areca.app", 1, false ) )
                .create()
                .onSuccess( repo -> instance = repo );
    }


    public static EntityRepository instance() {
        return Assert.notNull( instance, "ModelRepo.init() !?" );
    }


    public static UnitOfWork unitOfWork() {
        if (uow == null) {
            uow = instance().newUnitOfWork();
        }
        return uow;
    }

}
