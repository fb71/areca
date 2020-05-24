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
package areca.systemservice.client;

import areca.common.ProgressMonitor;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * Walk the hierarchy of webdav resources and notifies {@link HierarchyVisitor}.
 *
 * @author Falko Bräutigam
 */
public class HierarchyWalker {

    private static final Log log = LogFactory.getLog( HierarchyWalker.class );

    private SystemServiceClient client;

    private HierarchyVisitor    visitor;

    private ProgressMonitor     monitor;


    public HierarchyWalker( SystemServiceClient client, HierarchyVisitor visitor, ProgressMonitor monitor ) {
        this.client = client;
        this.visitor = visitor;
        this.monitor = monitor;
    }


    public void process( Path path ) {
        if (monitor.isCancelled()) {
            return;
        }
        client.fetchFolder( path, entries -> {
            if (visitor.visitFolder( path, entries )) {
                for (FolderEntry entry : entries) {
                    process( entry.path );
                }
            }
            return null;
        }, e -> visitor.onError( e ) );
    }

}
