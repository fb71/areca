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
package areca.ui.layout;

import java.util.logging.Logger;

import areca.ui.Position;
import areca.ui.Size;
import areca.ui.component.Property;
import areca.ui.component.UIComponent;
import areca.ui.component.UIComposite;


/**
 *
 * @author Falko Bräutigam
 */
public class RowLayout
        extends LayoutManager {

    private static final Logger LOG = Logger.getLogger( RowLayout.class.getName() );

    public Property<Integer>        margins = Property.create( this, "margins" );

    public Property<Integer>        spacing = Property.create( this, "spacing" );


    @Override
    public void layout( UIComposite composite ) {
        Size size = composite.size.get();
        LOG.info( "RowLayout: " + size );

        int componentTop = 0;
        for (UIComponent component : composite.components()) {
            RowConstraints constraints = component.layoutConstraints();
            component.size.set( Size.of( size.width(), constraints.height.get() ) );
            component.position.set( Position.of( 0, componentTop ) );

            componentTop += constraints.height.get();
        }
    }

}
