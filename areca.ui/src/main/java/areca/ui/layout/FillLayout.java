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

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Orientation;
import areca.ui.Position;
import areca.ui.Size;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;

/**
 *
 * @author falko
 */
public class FillLayout
        extends AbsoluteLayout {

    private static final Log LOG = LogFactory.getLog( FillLayout.class );

    /**
     * Returns a newly created instance width {@link #orientation} set to {@link Orientation#HORIZONTAL}.
     */
    public static FillLayout defaults() {
        return new FillLayout();
    }

    private Orientation             orientation = Orientation.HORIZONTAL;


    @Override
    public void layout( UIComposite composite ) {
        super.layout( composite );
        Size size = composite.clientSize.value();
        LOG.debug( "FillLayout: " + size );

        if (orientation == Orientation.HORIZONTAL) {
            int componentsMaxWidth = composite.components.values()
                    .map( c -> c.computeMinWidth( size.height() ) )
                    .reduce( Math::max ).orElse( 0 );

            int componentWidth = Math.max( size.width() / composite.components.size(), componentsMaxWidth );
            int count = 0;
            for (UIComponent component : orderedComponents( composite )) {
                component.size.set( Size.of( componentWidth, size.height() ) );
                component.position.set( Position.of( count++ * componentWidth, 0 ) );
            }
        }
        else {
            throw new RuntimeException( "not yet implemented." );
        }
    }

}
