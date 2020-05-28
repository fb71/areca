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
package areca.app.service.email;

import org.polymap.model2.runtime.UnitOfWork;

import areca.app.Main;
import areca.app.model.Message;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.systemservice.client.HierarchyVisitor;
import areca.systemservice.client.Path;
import areca.ui.component.Property;

/**
 *
 * @author Falko Bräutigam
 */
public class EmailFolderSynchronizer
        extends HierarchyVisitor {

    private static final Log log = LogFactory.getLog( EmailFolderSynchronizer.class );

    public Property<Boolean>        checkExisting = Property.create( this, "checkExisting", false );

    private UnitOfWork              uow;

    protected volatile int          count;


    @Override
    public boolean acceptsFolder( Path path ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }


    @Override
    public boolean acceptsFile( Path path ) {
        return path.lastPart().equals( "envelope.xml" );
    }


    @Override
    public void visitFile( Path path, Object content ) {
        count ++;
        try (
            UnitOfWork uow = Main.repo.supply().newUnitOfWork();
        ) {
            uow.createEntity( Message.class, null, proto -> {
                return proto;
            });
            DOMParser parser = DOMParser.create();
            uow.commit();
        }
    }


    @Override
    public void onError( Exception e ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

}
