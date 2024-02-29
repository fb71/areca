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
package areca.ui.pageflow;

import areca.common.Platform;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Position;
import areca.ui.component2.UIComposite;
import areca.ui.layout.AbsoluteLayout;

/**
 *
 * @author Falko Bräutigam
 */
public class PageStackLayout
        extends AbsoluteLayout {

    private static final Log LOG = LogFactory.getLog( PageStackLayout.class );

    private UIComposite     composite;

    @Override
    public void layout( @SuppressWarnings("hiding") UIComposite composite ) {
        super.layout( composite );
        this.composite = composite;

        // int zIndex = 0;
        for (var component : composite.components) {
            component.position.set( Position.of( 0, 0 ) );
            component.size.set( composite.clientSize.value() );
            // component.zIndex.set( zIndex++ );
        }
    }


    /**
     * Show the page open animation.
     *
     * @param origin The position where the action has its origin.
     */
    public void openLast( Position origin ) {
        // scale last one
        composite.components.values().last().ifPresent( top -> {
            top.cssClasses.add( "PageStackLayout-Top" );

            top.position.set( origin != null
                    //XXX ? origin.substract( composite.clientSize.value().divide( 1.2f ).divide( 2 ) )
                    ? Position.of( 0, 0 )
                    : Position.of( 0, 0 ) );

            //LOG.info( "Position: %s - %s -> %s", origin, composite.clientSize.value().divide( 1.2f ), top.position.value() );

            Platform.schedule( 200, () -> {
                top.bordered.set( true );
                top.cssClasses.remove( "PageStackLayout-Top" );
                top.position.set( Position.of( 0, 0 ) );

                Platform.schedule( 500, () -> {
                    top.bordered.set( false );
                });
            });
        });
    }

}
