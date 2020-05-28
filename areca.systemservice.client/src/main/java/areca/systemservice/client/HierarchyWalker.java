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
 * Simple, breadth-first, max-concurrent hierarchy walker of webdav resources,
 * notifying {@link HierarchyVisitor}.
 *
 * @author Falko Bräutigam
 */
public class HierarchyWalker {

    private static final Log log = LogFactory.getLog( HierarchyWalker.class );

    private SystemServiceClient client;

    private HierarchyVisitor    visitor;

    private ProgressMonitor     monitor;

    private ResponseFuture<Object,RuntimeException> result = new ResponseFuture<>();

    private volatile int        openRequests;


    public HierarchyWalker( SystemServiceClient client, HierarchyVisitor visitor, ProgressMonitor monitor ) {
        this.client = client;
        this.visitor = visitor;
        this.monitor = monitor;
    }


    public ResponseFuture<Object,RuntimeException> process( Path path ) {
        countOpenRequests( +1 );
        client.fetchFolder( path, entries -> {
            for (FolderEntry entry : entries) {
                if (isCancelled()) {
                    break;
                }
                else if (entry.isFolder()) {
                    if (visitor.acceptsFolder( entry.path )) {
                        process( entry.path );
                    }
                }
                else {
                    if (visitor.acceptsFile( entry.path )) {
                        countOpenRequests( +1 );
                        client.fetchFile( entry.path, content -> {
                            visitor.visitFile( entry.path, content );
                            countOpenRequests( -1 );
                            return null;
                        }, e -> visitor.onError( e ) );
                    }
                }
            }
            countOpenRequests( -1 );
            return null;
        }, e -> visitor.onError( e ) );
        return result;
    }


    protected void countOpenRequests( int count ) {
        openRequests += count;
        if (openRequests == 0) {
            result.setValue( null );
        }
    }


    protected boolean isCancelled() {
        if (monitor.isCancelled()) {
            // XXX sync result
            return true;
        }
        if (result.isCancelled()) {
            monitor.done();
            return true;
        }
        return false;
    }

}
