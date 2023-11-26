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

    /**
     * Returns a new instance with defaults values set.
     */
    public static RowLayout defaults() {
        return new RowLayout();
    }

    /**
     * Returns a new instance with {@link #fillHeight} and {@link #fillWidth} true.
     */
    public static RowLayout filled() {
        return new RowLayout().fillHeight.set( true ).fillWidth.set( true );
    }

    /**
     * Returns a new instance with {@link #vertical()} true.
     */
    public static RowLayout verticals() {
        return new RowLayout().vertical();
    }

    // instance *******************************************

    public ReadWrite<RowLayout,Size>        margins = Property.rw( this, "margins", Size.of( 0, 0 ) );

    public RowLayout margins( Size value ) {
        this.margins.set( value );
        return this;
    }

    public ReadWrite<RowLayout,Integer>     spacing = Property.rw( this, "spacing", 0 );

    public RowLayout spacing( int value ) {
        this.spacing.set( value );
        return this;
    }

    public ReadWrite<RowLayout,Orientation> orientation = Property.rw( this, "orientation", HORIZONTAL );

    public RowLayout vertical() {
        orientation.set( Orientation.VERTICAL );
        return this;
    }

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

    public RowLayout fillWidth( boolean value ) {
        fillWidth.set( value );
        return this;
    }


    @Override
    public void layout( UIComposite composite ) {
        Size size = composite.clientSize.opt().orElse( Size.of( 50, 50 ) );
        LOG.debug( "RowLayout: " + size );

        Size clientSize = Size.of(
                size.width() - (margins.value().width() * 2),
                size.height() - (margins.value().height() * 2) );

        // VERTICAL
        if (orientation.value() == VERTICAL) {
            // the width to be filled by components without width set
            int freeHeight = clientSize.height();
            int freeCount = 0;
            for (var c : composite.components.value()) {
                var constraints = c.<RowConstraints>layoutConstraints().orElse( new RowConstraints() );
                freeHeight -= constraints.height.opt().orElse( 0 ) + spacing.value();
                freeCount += constraints.height.opt().isPresent() ? 0 : 1;
            }
            int freeComponentHeight = (freeHeight + spacing.value()) / freeCount;

            // components
            int cTop = margins.value().height();
            for (UIComponent c : orderedComponents( composite )) {
                var constraints = c.<RowConstraints>layoutConstraints().orElse( new RowConstraints() );

                // height
                int cHeight = constraints.height.opt().orElse(
                        fillHeight.value() ? freeComponentHeight : c.computeMinHeight( clientSize.width() ) );

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

            // the width to be filled by components without width set
            int freeWidth = clientSize.width();
            int freeCount = 0;
            for (var c : composite.components.value()) {
                var constraints = c.<RowConstraints>layoutConstraints().orElse( new RowConstraints() );
                freeWidth -= constraints.width.opt().orElse( 0 ) + spacing.value();
                freeCount += constraints.width.opt().isPresent() ? 0 : 1;
            }
            int freeComponentWidth = (freeWidth + spacing.value()) / freeCount;
            LOG.debug( "%s %s %s %s", composite.components.size(), freeCount, freeWidth, freeComponentWidth );

            // components
            for (UIComponent c : orderedComponents( composite )) {
                var constraints = c.<RowConstraints>layoutConstraints().orElse( new RowConstraints() );

                // width
                int cWidth = constraints.width.opt().orElse(
                        fillWidth.value() ? freeComponentWidth : c.computeMinWidth( clientSize.height() ) );

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
