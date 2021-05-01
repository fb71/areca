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
package areca.ui.gesture;

import java.util.EventObject;

import areca.common.event.EventListener;
import areca.common.event.EventManager;
import areca.common.event.EventManager.EventHandlerInfo;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Position;
import areca.ui.Property;
import areca.ui.Property.ReadOnly;
import areca.ui.component.UIComponent;
import areca.ui.html.HtmlEventTarget.EventType;
import areca.ui.html.HtmlEventTarget.HtmlMouseEvent;

/**
 *
 */
public class PanGesture {

    static final Log LOG = LogFactory.getLog( PanGesture.class );

    public static final int EDGE_THRESHOLD = 50;

    public enum Status {
        START, MOVE, END;
    }

    public class PanEvent
            extends EventObject {

        public ReadOnly<UIComponent,Status> status;

        public ReadOnly<UIComponent,Position> delta;

        public ReadOnly<UIComponent,Position> lastDelta;

        public ReadOnly<UIComponent,Position> clientPos;

        public PanEvent( HtmlMouseEvent ev, Status status ) {
            super( component );
            this.status = Property.create( component, "type", status );
            this.delta = Property.create( component, "delta", ev.clientPosition.get().substract( startPos ) );
            this.lastDelta = Property.create( component, "lastDelta", ev.clientPosition.get().substract( lastPos ) );
            this.clientPos = Property.create( component, "clientPos", ev.clientPosition.get() );
        }

        @Override
        public UIComponent getSource() {
            return (UIComponent)super.getSource();
        }
    }

    // instance *******************************************

    private UIComponent     component;

    private boolean         isDown;

    private Position        startPos;

    private long            lastTime;

    private Position        lastPos;

    public PanGesture( UIComponent component ) {
        this.component = component;
        //            component.htmlElm.listeners.add( EventType.TOUCHSTART, ev -> {
        //                log.info( "TOUCH: " + ev );
        //                onStart( ev );
        //            });
        //            component.htmlElm.listeners.add( EventType.TOUCHMOVE, ev -> {
        //                log.info( "TOUCH: " + ev );
        //                onMove( ev );
        //            });
        //            component.htmlElm.listeners.add( EventType.TOUCHEND, ev -> {
        //                log.info( "TOUCH: " + ev );
        //                onEnd( ev );
        //            });
        component.htmlElm.listeners.add( EventType.MOUSEDOWN, ev -> {
            onStart( ev );
        });
        component.htmlElm.listeners.add( EventType.MOUSEMOVE, ev -> {
            onMove( ev );
        });
        component.htmlElm.listeners.add( EventType.MOUSEUP, ev -> {
            onEnd( ev );
        });
    }


    public EventHandlerInfo onEvent( EventListener<PanEvent> l ) {
        return EventManager.instance()
                .subscribe( l )
                .performIf( ev -> ev instanceof PanEvent && ev.getSource() == component)
                .disposeIf( ev -> component.isDisposed() );
    }

    //

    protected void onStart( HtmlMouseEvent ev ) {
        LOG.info( "DOWN: " + ev );
        isDown = true;
        lastTime = 0;
        startPos = lastPos = ev.clientPosition.get();
    }


    protected void onMove( HtmlMouseEvent ev ) {
        if (isDown) {
            LOG.info( "MOVE: " + ev );
            // edge detection without throttle
            if (ev.clientPosition.get().y > (component.size.get().height() - EDGE_THRESHOLD)) {
                onEnd( ev );
            }
            else if ((System.currentTimeMillis() - lastTime) < 100) {
                LOG.info( "IGNORED" );
            }
            else {
                // fire (deferred first/start event)
                EventManager.instance().publish( new PanEvent( ev, lastTime == 0 ? Status.START : Status.MOVE ) );

                lastPos = ev.clientPosition.get();
                lastTime = System.currentTimeMillis();
            }
        }
    }


    protected void onEnd( HtmlMouseEvent ev ) {
        if (isDown && lastTime > 0) {
            LOG.info( "UP: " + ev );
            EventManager.instance().publish( new PanEvent( ev, Status.END ) );
        }

        isDown = false;
        startPos = null;
        lastTime = 0;
        lastPos = null;
    }

}