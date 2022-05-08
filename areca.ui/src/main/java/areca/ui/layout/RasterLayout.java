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
package areca.ui.layout;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Position;
import areca.ui.Property;
import areca.ui.Size;
import areca.ui.component2.UIComposite;
import areca.ui.Property.ReadWrite;

/**
 *
 * @author Falko Bräutigam
 */
public class RasterLayout
         extends LayoutManager {

    private static final Log LOG = LogFactory.getLog( RasterLayout.class );

    public ReadWrite<RasterLayout,Integer> spacing = Property.create( this, "spacing", 5 );


    @Override
    public void layout( UIComposite composite ) {
        Size size = composite.clientSize.value();

//        composite.components().sequence().forEach( c -> {
//            c.position.set( Position.of( 1, 1 ) );
//            c.size.set( Size.of( 0, 0 ) );
//        } );

        var cols = size.width() / 50;
        var i = 0;
        for (var component : composite.components) {
            component.size.set( Size.of( 48, 48 ) );
            component.position.set( Position.of( 53 * (i % cols), 53 * (i / cols) ) );
            i++;
        }
    }

}
