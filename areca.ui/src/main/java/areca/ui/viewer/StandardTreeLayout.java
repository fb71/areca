/*
 * Copyright (C) 2025, the @authors. All rights reserved.
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
package areca.ui.viewer;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.UIComposite;
import areca.ui.viewer.TreeViewer.TreeViewerLayout;

/**
 * A standard expandable {@link TreeViewer tree}.
 *
 * @author Falko Bräutigam
 */
public class StandardTreeLayout<V>
        extends TreeViewerLayout<V> {

    private static final Log LOG = LogFactory.getLog( StandardTreeLayout.class );

    @Override
    public UIComposite init( TreeViewer<V> viewer ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }


    @Override
    public void expand( TreeViewer<V>.Level level ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }


    @Override
    public void collapse( TreeViewer<V>.Level l ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

}
