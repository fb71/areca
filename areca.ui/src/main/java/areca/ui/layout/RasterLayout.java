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

import java.util.Collection;

import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Position;
import areca.ui.Size;
import areca.ui.component2.Property;
import areca.ui.component2.Property.ReadWrite;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;

/**
 *
 * @author Falko Bräutigam
 */
public class RasterLayout
         extends LayoutManager {

    private static final Log LOG = LogFactory.getLog( RasterLayout.class );

    public ReadWrite<RasterLayout,Integer>  spacing = Property.rw( this, "spacing", 0 );

    public ReadWrite<RasterLayout,Size>     margins = Property.rw( this, "margins", Size.of( 0, 0 ) );

    public ReadWrite<RasterLayout,Integer>  columns = Property.rw( this, "columns" );

    public ReadWrite<RasterLayout,Size>     itemSize = Property.rw( this, "itemSize" );


    @Override
    public void layout( UIComposite composite ) {
        doLayout( composite, composite.components.value() );
    }


    public void doLayout( UIComposite composite, Collection<? extends UIComponent> components ) {
        LOG.debug( "Components: %s, %s", components.size(), composite.clientSize.opt().orElse( Size.of( -1, -1 ) ) );
        Size size = composite.clientSize.opt().orElse( Size.of( 50, 50 ) ).substract( margins.value() );

        var cWidth = itemSize.value().width();
        var cHeight = itemSize.value().height();
        var cols = size.width() / (cWidth + spacing.value());

        Sequence.of( components ).forEach( (child,i) -> {
            var col = i % cols;
            var line = i /cols;
            child.size.set( Size.of( cWidth, cHeight ) );
            child.position.set( Position.of(
                    margins.value().width() + ((cWidth + spacing.value()) * col),
                    margins.value().height() + ((cHeight + spacing.value()) * line) ) );
        });
    }

}
