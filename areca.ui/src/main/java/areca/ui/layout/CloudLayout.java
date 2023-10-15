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
public class CloudLayout
        extends LayoutManager {

    private static final Log LOG = LogFactory.getLog( CloudLayout.class );

    public static CloudLayout withSpacing( int spacing ) {
        return new CloudLayout().spacing.set( spacing );
    }

    // instance *******************************************

    public ReadWrite<CloudLayout,Integer> spacing = Property.rw( this, "spacing", 10 );

    public ReadWrite<CloudLayout,Integer> componentHeight = Property.rw( this, "componentHeight", 30 );

    @Override
    public void layout( UIComposite composite ) {
        var cTop = 0;
        var cLeft = 0;
        for (UIComponent c : orderedComponents( composite )) {
            var cHeight = componentHeight.get();
            var cWidth = c.<RowConstraints>layoutConstraints()
                    .orElse( RowConstraints.width( c.computeMinWidth( cHeight ) ) )
                    .width.get();

            if (cLeft + cWidth > composite.size.$().width()) {
                cTop += spacing.$() + cHeight;
                cLeft = 0;
            }
            c.position.set( Position.of( cLeft, cTop) );
            c.size.set( Size.of( cWidth, cHeight ) );

            cLeft += spacing.$() + cWidth;
        }
    }

}
