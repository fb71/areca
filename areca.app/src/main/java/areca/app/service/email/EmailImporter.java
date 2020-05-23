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

import java.util.List;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.systemservice.client.FolderEntry;
import areca.systemservice.client.Path;
import areca.systemservice.client.WebdavHierarchyVisitor;

/**
 *
 * @author Falko Bräutigam
 */
public class EmailImporter
        extends WebdavHierarchyVisitor {

    private static final Log log = LogFactory.getLog( EmailImporter.class );

    @Override
    public boolean visitFolder( Path path, List<FolderEntry> entries ) {
        log.info( "" );
        return true;
    }

    @Override
    public void visitFile( Path path, Object content ) {
    }

    @Override
    public void onError( Exception e ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

}
