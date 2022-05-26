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
package areca.ui.viewer;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.UIComponent;

/**
 *
 * @author Falko Bräutigam
 */
public class ListViewer
        extends Viewer<SingleValueAdapter<?>> {

    private static final Log log = LogFactory.getLog( ListViewer.class );

    @Override
    public UIComponent create() {
        throw new RuntimeException( "not implemented yet." );
    }

    @Override
    public void store() {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public void load() {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

}
