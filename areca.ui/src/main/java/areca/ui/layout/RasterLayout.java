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
import areca.ui.Size;
import areca.ui.component2.Property;
import areca.ui.component2.Property.ReadWrite;
import areca.ui.component2.UIComposite;

/**
 *
 * @author Falko Bräutigam
 */
public class RasterLayout
         extends LayoutManager {

    private static final Log LOG = LogFactory.getLog( RasterLayout.class );

    public static RasterLayout withComponentSize( Size componentSize ) {
        return new RasterLayout().componentSize.set( componentSize );
    }

    public static RasterLayout withComponentSize( int width, int height ) {
        return new RasterLayout().componentSize.set( Size.of( width, height ) );
    }

    public static RasterLayout colums( int colums ) {
        return new RasterLayout().columns.set( colums );
    }

    // instance *******************************************

    public ReadWrite<RasterLayout,Integer>  spacing = Property.rw( this, "spacing", 0 );

    public RasterLayout spacing( int value ) {
        this.spacing.set( value );
        return this;
    }

    public ReadWrite<RasterLayout,Size>     margins = Property.rw( this, "margins", Size.of( 0, 0 ) );

    public RasterLayout margins( Size value ) {
        this.margins.set( value );
        return this;
    }

    public ReadWrite<RasterLayout,Integer>  columns = Property.rw( this, "columns" );

    public ReadWrite<RasterLayout,Size>     componentSize = Property.rw( this, "itemSize" );


    @Override
    public void layout( UIComposite composite ) {
        composite.clientSize.opt().ifPresent( size -> {
            size = size.substract( margins.$() );

            var cWidth = -1;
            var cHeight = -1;
            var cols = -1;

            if (componentSize.opt().isPresent()) {
                cWidth = componentSize.value().width();
                cHeight = componentSize.value().height();
                cols = size.width() / (cWidth + spacing.value());
            }
            if (columns.opt().isPresent()) {
                cWidth = (size.width() - ((columns.$()-1) * spacing.$())) / columns.$();
                cHeight = cWidth;
                cols = columns.$();
            }

            var i = 0;
            for (var c : orderedComponents( composite )) {
                var col = i % cols;
                var line = i / cols;
                c.size.set( Size.of( cWidth, cHeight ) );
                c.position.set( Position.of(
                        margins.$().width() + ((cWidth + spacing.$()) * col),
                        margins.$().height() + ((cHeight + spacing.$()) * line) ) );
                i++;
            }
        });
    }

}
