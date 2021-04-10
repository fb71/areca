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
package areca.ui.pageflow;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Position;
import areca.ui.component.UIComposite;
import areca.ui.layout.LayoutManager;

/**
 *
 * @author Falko Bräutigam
 */
public class PageStackLayout
        extends LayoutManager {

    private static final Log log = LogFactory.getLog( PageStackLayout.class );

    @Override
    public void layout( UIComposite composite ) {
        var size = composite.size.get();

        int zIndex = 0;
        for (var component : composite.components()) {
            component.size.set( size );
            component.position.set( Position.of( 0, 0 ) );
            // component.zIndex.set( zIndex++ );
        }
    }

}
