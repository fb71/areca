/*
 * Copyright (C) 2025, the @authors. All rights reserved.
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

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Position;
import areca.ui.component2.UIComposite;
import areca.ui.gesture.PanGesture;

/**
 * Close/peek the top page of the {@link Pageflow}.
 *
 * @author Falko Bräutigam
 */
class PageCloseGesture
        extends PanGesture {

    private static final Log LOG = LogFactory.getLog( Pageflow.class );

    private static final float PEEK_DISTANCE_PX = 200f;

    private PageflowImpl pageflow;

    private Position startPos;


    public PageCloseGesture( PageflowImpl pageflow, UIComposite component ) {
        super( component );
        this.pageflow = pageflow;
        on( ev -> {
            var top = pageflow.pages.peek().ui;
            LOG.debug( "PageCloseGesture: top = %s, status = %s, delta = %s", top, ev.status(), "???"); //ev.delta() != null ? ev.delta() : "???" );
            switch (ev.status()) {
                case START: {
                    startPos = top.position.value();
                    top.cssClasses.add( "Paning" );
                    break;
                }
                case MOVE: {
                    //top.bordered.set( true );
                    top.position.set( Position.of( startPos.x, startPos.y + ev.delta().y ) );
                    top.opacity.set( Math.max( 0.2f, (PEEK_DISTANCE_PX - ev.delta().y) / PEEK_DISTANCE_PX ) );
                    break;
                }
                case END: {
                    top.opacity.set( null );
                    top.cssClasses.remove( "Paning" );

                    // close
                    if (ev.clientPos().y > (component.clientSize.value().height() - EDGE_THRESHOLD)) {
                        Pageflow.current().close( this.pageflow.pages.peek().page );
                    }
                    // reset
                    else {
                        top.position.set( startPos );
                        //Platform.schedule( 750, () -> top.bordered.set( false ) );
                    }
                }
            }
        });
    }
}