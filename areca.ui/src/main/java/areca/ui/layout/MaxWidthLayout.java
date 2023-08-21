/*
 * Copyright (C) 2023, the @authors. All rights reserved.
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

import areca.common.Assert;
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
public class MaxWidthLayout
        extends LayoutManager {

    private static final Log LOG = LogFactory.getLog( MaxWidthLayout.class );

    public static MaxWidthLayout width( int maxWidth ) {
        return new MaxWidthLayout().width.set( maxWidth );
    }

    public ReadWrite<MaxWidthLayout,Integer>    width = Property.rw( this, "width", -1 );

    /**
     * The child component fills the vertical space of the composite. Default:
     * {@link Boolean#FALSE}
     */
    public ReadWrite<MaxWidthLayout,Boolean>    fillHeight = Property.rw( this, "fillHeight", false );


    protected MaxWidthLayout() { }

    @Override
    public void layout( UIComposite composite ) {
        Assert.that( composite.components.size() <= 1, getClass().getSimpleName() + " does not allow more than 1 component");
        composite.components.values().first().ifPresent( c -> {
            var clientWidth = composite.clientSize.get().width();
            var clientHeight = composite.clientSize.get().height();

            int componentWidth = Math.min( clientWidth, width.$() );
            c.size.set( Size.of( componentWidth, fillHeight.$() ? clientHeight : c.computeMinHeight( componentWidth ) ) );

            var margin = (clientWidth - componentWidth) / 2;
            c.position.set( Position.of( margin, 0 ) );
        });
    }

}
