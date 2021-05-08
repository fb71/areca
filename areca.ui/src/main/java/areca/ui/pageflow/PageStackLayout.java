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
import areca.ui.component.UIComposite;
import areca.ui.html.HtmlEventTarget.EventType;
import areca.ui.html.HtmlEventTarget.ListenerHandle;
import areca.ui.layout.LayoutManager;

/**
 *
 * @author Falko Bräutigam
 */
public class PageStackLayout
        extends LayoutManager {

    private static final Log LOG = LogFactory.getLog( PageStackLayout.class );

    private Position        mousePos;

    private ListenerHandle  mouseMoveHandle;

    @Override
    public void layout( UIComposite composite ) {
        if (mouseMoveHandle == null) {
            mouseMoveHandle = composite.htmlElm.listeners.add( EventType.MOUSEMOVE, ev ->
                    mousePos = ev.clientPosition.get() );
        }

        var size = composite.clientSize.get();

        // int zIndex = 0;
        for (var component : composite.components) {
            component.position.set( Position.of( 0, 0 ) );
            component.size.set( size );
            // component.zIndex.set( zIndex++ );
        }

        // scale last one
        composite.components.sequence().last().ifPresent( top -> {
            top.htmlElm.styles.set( "transition", "none" );
            top.htmlElm.styles.set( "transform", "scale(0.01)" );
            //top.htmlElm.styles.set( "opacity", 0 );
            top.position.set( mousePos != null
                    ? mousePos.substract( size.divide( 2 ) )
                    : Position.of( 0, 0 ) );

            Platform.instance().schedule( 300, () -> {
                top.bordered.set( true );
                top.htmlElm.styles.remove( "transition" );
                top.htmlElm.styles.remove( "transform" );
                top.htmlElm.styles.remove( "opacity" );
                top.position.set( Position.of( 0, 0 ) );

                Platform.instance().schedule( 500, () -> {
                    top.bordered.set( false );
                });
            });
        });
    }

}
