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
import areca.ui.gesture.PanGesture;

/**
 * Close/peek the top page of the {@link Pageflow}.
 *
 * @author Falko Bräutigam
 */
class PageCloseGesture
        extends PanGesture {

    private static final Log LOG = LogFactory.getLog( PageCloseGesture.class );

    private static final float PEEK_DISTANCE_PX = 200f;

    private Position        startPos;

    public PageCloseGesture( UIComposite component ) {
        super( component );
        onEvent( ev -> {
            LOG.info( "%s", ev.delta.get() );
            var top = component.components.sequence().last().get();
            switch (ev.status.get()) {
                case START: {
                    startPos = top.position.get();
                    top.cssClasses.add( "Paned" );
                    break;
                }
                case MOVE: {
                    top.bordered.set( true );
                    top.position.set( Position.of(
                            startPos.x(),
                            startPos.y() + ev.delta.get().y() ) );
                    float opacity = (PEEK_DISTANCE_PX - ev.delta.get().y()) / PEEK_DISTANCE_PX;
                    top.htmlElm.styles.set( "opacity", opacity );
                    break;
                }
                case END: {
                    top.position.set( startPos );
                    top.htmlElm.styles.remove( "opacity" );
                    top.cssClasses.remove( "Paned" );
                    Platform.instance().schedule( 1000, () -> top.bordered.set( false ) );
                }
            }
        });
    }
}
