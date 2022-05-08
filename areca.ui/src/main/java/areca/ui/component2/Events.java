/*
 * Copyright (C) 2022, the @authors. All rights reserved.
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
package areca.ui.component2;

import java.util.ArrayList;
import java.util.EventObject;

import areca.common.base.Consumer.RConsumer;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.Property.ReadWrites;

/**
 *
 * @author Falko Bräutigam
 */
public class Events
        extends ReadWrites<UIComponent,Events.EventHandler> {

    private static final Log LOG = LogFactory.getLog( Events.class );

    public enum EventType {
        SELECT, ACTION, POINTER_ENTER, POINTER_LEAVE, POINTER_MOVE
//        CLICK,
//        CONTEXTMENU,
//        DBLCLICK,
//        MOUSEDOWN,
//        MOUSEENTER,
//        MOUSELEAVE,
//        MOUSEMOVE,
//        MOUSEOUT,
//        MOUSEOVER,
//        MOUSEUP,
//        TOUCHSTART,
//        TOUCHEND,
//        TOUCHMOVE,
//        TOUCHCANCEL;
    }

    public static class EventHandler {
        public EventType                    type;
        public RConsumer<UIEvent>           consumer;
    }


    // instance *******************************************

    protected Events( UIComponent component ) {
        super( component, "events" );
        rawSet( new ArrayList<>() );
    }


    protected void dispose() {
        // XXX remove???
    }


    public void on( EventType _type, RConsumer<UIEvent> _handler ) {
        add( new EventHandler() {{this.type = _type; this.consumer = _handler;}} );
    }


    /**
     *
     */
    public static class UIEvent
            extends EventObject {

        public Object           htmlEv;

        public EventType        type;

        public UIEvent( UIComponent source ) {
            super( source );
        }

        @Override
        public UIComponent getSource() {
            return (UIComponent)super.getSource();
        }
    }
}
