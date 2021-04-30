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
package areca.ui.html;

import areca.common.base.Consumer.RConsumer;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Position;
import areca.ui.Property.ReadOnly;

/**
 *
 * @author Falko Bräutigam
 */
public abstract class HtmlEventTarget
        extends HtmlNode {

    private static final Log log = LogFactory.getLog( HtmlEventTarget.class );

    public enum EventType {
        CLICK,
        CONTEXTMENU,
        DBLCLICK,
        MOUSEDOWN,
        MOUSEENTER,
        MOUSELEAVE,
        MOUSEMOVE,
        MOUSEOUT,
        MOUSEOVER,
        MOUSEUP,
        TOUCHSTART,
        TOUCHEND,
        TOUCHMOVE,
        TOUCHCANCEL;

        public String html() {
            return name().toLowerCase();
        }
    }

    public HtmlEventListeners listeners;


    /**
     *
     */
    public static abstract class HtmlEventListeners {

        public abstract ListenerHandle add( EventType type, RConsumer<HtmlMouseEvent> handle );

        public abstract void remove( ListenerHandle handle );

        public abstract void clear();
    }

    /**
     *
     */
    public static class ListenerHandle {

    }

    /**
     *
     */
    public static class HtmlEvent {

    }

    /**
     *
     */
    public static class HtmlMouseEvent
            extends HtmlEvent {

        public ReadOnly<HtmlMouseEvent,Position> clientPosition;
    }
}
