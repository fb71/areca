/*
 * Copyright (C) 2020-2022, the @authors. All rights reserved.
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

import static areca.ui.Orientation.HORIZONTAL;
import static areca.ui.Orientation.VERTICAL;

import areca.common.Assert;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Orientation;
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
public class RowLayout
        extends LayoutManager {

    private static final Log LOG = LogFactory.getLog( RowLayout.class );

    public ReadWrite<RowLayout,Size>        margins = Property.rw( this, "margins", Size.of( 0, 0 ) );

    public ReadWrite<RowLayout,Integer>     spacing = Property.rw( this, "spacing", 0 );

    public ReadWrite<RowLayout,Orientation> orientation = Property.rw( this, "orientation", HORIZONTAL );

    /**
     * {@link Orientation#HORIZONTAL}: all components have the same height so that the
     * entiry client size of the composite is filled. Default: {@link Boolean#FALSE}
     */
    public ReadWrite<RowLayout,Boolean>     fillHeight = Property.rw( this, "fillHeight", false );

    /**
     * {@link Orientation#HORIZONTAL}: all components have the same width so that the
     * entiry client size is filled. Individual settings via
     * {@link RowConstraints#width} are ignored. Default: {@link Boolean#FALSE}
     */
    public ReadWrite<RowLayout,Boolean>     fillWidth = Property.rw( this, "fillWidth", false );


    @Override
    public void layout( UIComposite composite ) {
        Size size = composite.clientSize.opt().orElse( Size.of( 50, 50 ) );
        LOG.debug( "RowLayout: " + size );

        Size clientSize = Size.of(
                size.width() - (margins.value().width() * 2),
                size.height() - (margins.value().height() * 2) );

        // VERTICAL
        if (orientation.value() == VERTICAL) {
            int cTop = margins.value().height();
            for (UIComponent c : orderedComponents( composite )) {
                var constraints = c.<RowConstraints>layoutConstraints().orElse( new RowConstraints() );

                // height
                Assert.that( !(fillHeight.value() && constraints.height.opt().isPresent()) );
                int cHeight = fillHeight.value()
                        ? ((clientSize.height() + spacing.value()) / composite.components.size()) - spacing.value()
                        : constraints.height.opt().orElse( c.computeMinHeight( clientSize.width() ) );

                // width
                int cWidth = fillWidth.value()
                        ? clientSize.width()
                        : constraints.width.opt().orElse( c.computeMinWidth( clientSize.height() ) );

                c.size.set( Size.of( cWidth, cHeight ) );
                c.position.set( Position.of( margins.value().width(), cTop ) );

                cTop += cHeight + spacing.value();
            }
        }
        // HORIZONTAL
        else {
            int cLeft = margins.value().width();
            for (UIComponent c : orderedComponents( composite )) {
                var constraints = c.<RowConstraints>layoutConstraints().orElse( new RowConstraints() );

                // width
                Assert.that( !(fillWidth.value() && constraints.width.opt().isPresent()) );
                int cWidth = fillWidth.value()
                        ? ((clientSize.width() + spacing.value()) / composite.components.size()) - spacing.value()
                        : constraints.width.opt().orElse( c.computeMinWidth( clientSize.height() ) );

                // height
                int cHeight = fillHeight.value()
                        ? clientSize.height()
                        : constraints.height.opt().orElse( c.computeMinHeight( clientSize.width() ) );

                c.size.set( Size.of( cWidth, cHeight ) );
                c.position.set( Position.of( cLeft, margins.value().height() ) );

                cLeft += cWidth + spacing.value();
            }
        }
    }

}
