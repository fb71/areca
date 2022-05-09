/*
 * Copyright (C) 2021-2022, the @authors. All rights reserved.
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
import areca.common.event.EventManager.EventHandlerInfo;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Position;
import areca.ui.component2.Events;
import areca.ui.component2.Events.EventType;
import areca.ui.component2.Events.UIEvent;
import areca.ui.component2.UIComponent;

/**
 * Decorates a {@link UIComponent} with the ability to handle pan gestures
 * and publish {@link PanEvent}s.
 */
public class PanGesture {

    static final Log LOG = LogFactory.getLog( PanGesture.class );

    public static final int EDGE_THRESHOLD = 50;

    public enum Status {
        START, MOVE, END;
    }

    /**
     *
     */
    public class PanEvent
            extends EventObject {

        private UIEvent     ev;

        private Status      status;

        public PanEvent( UIEvent ev, Status status ) {
            super( component );
            this.status = status;
            this.ev = ev;
        }

        @Override
        public UIComponent getSource() {
            return (UIComponent)super.getSource();
        }

        public Status status() {
            return Status.END;
        }

        public Position delta() {
            return ev.clientPos().substract( startPos );
        }

        public Position lastDelta() {
            return ev.clientPos().substract( lastPos );
        }

        public Position clientPos() {
            return ev.clientPos();
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
        component.events.on( EventType.TOUCHSTART, ev -> {
            LOG.debug( "TOUCH: START " + ev );
            onStart( ev );
        });
        component.events.on( EventType.TOUCHMOVE, ev -> {
            LOG.debug( "TOUCH: MOVE " + ev.clientPos() );
            onMove( ev );
        });
        component.events.on( EventType.TOUCHEND, ev -> {
            LOG.debug( "TOUCH: END " + ev );
            onEnd( ev );
        });
        component.events.on( EventType.MOUSEDOWN, ev -> {
            onStart( ev );
        });
        component.events.on( EventType.MOUSEMOVE, ev -> {
            onMove( ev );
        });
        component.events.on( EventType.MOUSEUP, ev -> {
            onEnd( ev );
        });
    }


    public EventHandlerInfo on( EventListener<PanEvent> l ) {
        return Events.manager
                .subscribe( l )
                .performIf( ev -> ev instanceof PanEvent && ev.getSource() == component)
                .disposeIf( ev -> component.isDisposed() );
    }

    //

    protected void onStart( UIEvent ev ) {
        LOG.debug( "DOWN: " + ev );
        isDown = true;
        lastTime = 0;
        startPos = lastPos = ev.clientPos();
    }


    protected void onMove( UIEvent ev ) {
        if (isDown) {
            LOG.debug( "MOVE: " + ev );
            // edge detection without throttle
            if (ev.clientPos().y > (component.size.value().height() - EDGE_THRESHOLD)) {
                onEnd( ev );
            }
            else if ((System.currentTimeMillis() - lastTime) < 100) {
                LOG.debug( "IGNORED" );
            }
            else {
                // fire (deferred first/start event)
                Events.manager.publish( new PanEvent( ev, lastTime == 0 ? Status.START : Status.MOVE ) );

                lastPos = ev.clientPos();
                lastTime = System.currentTimeMillis();
            }
        }
    }


    protected void onEnd( UIEvent ev ) {
        if (isDown && lastTime > 0) {
            LOG.debug( "UP: " + ev );
            Events.manager.publish( new PanEvent( ev, Status.END ) );
        }

        isDown = false;
        startPos = null;
        lastTime = 0;
        lastPos = null;
    }

}