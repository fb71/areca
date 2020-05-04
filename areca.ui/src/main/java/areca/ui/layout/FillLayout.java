/*
 * Copyright (C) 2019, the @authors. All rights reserved.
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
package areca.ui.layout;

import java.util.logging.Logger;

import areca.ui.Orientation;
import areca.ui.Position;
import areca.ui.Size;
import areca.ui.component.UIComponent;
import areca.ui.component.UIComposite;

/**
 *
 * @author falko
 */
public class FillLayout
        extends LayoutManager {

    private static final Logger LOG = Logger.getLogger( FillLayout.class.getSimpleName() );

    private Orientation             orientation = Orientation.HORIZONTAL;


    @Override
    public void layout( UIComposite composite ) {
        Size size = composite.size.get();
        LOG.info( "Composite: " + size );

        if (orientation == Orientation.HORIZONTAL) {
            int compositeMaxWidth = composite.components().stream()
                    .map( c -> c.size.get().width() ).reduce( Math::max ).orElse( 0 );
            int componentWidth = Math.max( size.width() / composite.components().size(), compositeMaxWidth );
            int count = 0;
            for (UIComponent component : composite.components()) {
                component.size.set( Size.of( componentWidth, size.height() ) );
                component.position.set( Position.of( count++ * componentWidth, 0 ) );
            }
        }
        else {
            throw new RuntimeException( "not yet implemented." );
        }
    }

}
