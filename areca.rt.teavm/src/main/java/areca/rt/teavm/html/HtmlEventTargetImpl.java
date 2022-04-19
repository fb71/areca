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
package areca.rt.teavm.html;

import static areca.common.base.With.with;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.EventTarget;
import org.teavm.jso.dom.events.MouseEvent;

import areca.common.base.Consumer.RConsumer;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Position;
import areca.ui.Property;
import areca.ui.html.HtmlEventTarget;
import areca.ui.html.HtmlEventTarget.EventType;
import areca.ui.html.HtmlEventTarget.HtmlEventListeners;
import areca.ui.html.HtmlEventTarget.HtmlMouseEvent;
import areca.ui.html.HtmlEventTarget.ListenerHandle;

/**
 *
 * @author Falko Bräutigam
 */
public class HtmlEventTargetImpl {

    private static final Log LOG = LogFactory.getLog( HtmlEventTargetImpl.class );


    public static void init( HtmlEventTarget elm, EventTarget delegate ) {
        elm.listeners = new HtmlEventListeners() {
            @Override
            public ListenerHandle add( EventType type, RConsumer<HtmlMouseEvent> handler ) {
                return _add( type, ev -> {
                    ev.stopPropagation();
                    ev.preventDefault();
                    // touch
                    if (ev.getType().startsWith( "touch" )) {
                        TouchEvent tev = ev.cast();
                        //LOG.info( "DOM: " + ev.getType() + " : " + mev.getTouches().item( 0 ).getClientX() );
                        Position pos = tev.getTouches().getLength() > 0
                                ? with( tev.getTouches().item( 0 ) ).map( t -> Position.of( t.getClientX(), t.getClientY() ) )
                                : Position.of( 0, 0 );
                        handler.accept( new HtmlMouseEvent() {{
                            clientPosition = Property.create( this, "clientPos", pos );
                        }});
                    }
                    // mouse
                    else {
                        handler.accept( HtmlMouseEventImpl.create( (MouseEvent)ev ) );
                    }
                });
            }
            protected ListenerHandle _add( EventType type, EventListener<?> listener ) {
                delegate.addEventListener( type.html(), listener );
                return new ListenerHandleImpl( type, listener );
            }
            @Override
            public void remove( ListenerHandle handle ) {
                delegate.removeEventListener( ((ListenerHandleImpl)handle).type.html(), ((ListenerHandleImpl)handle).listener );
            }
            @Override
            public void clear() {
                LOG.debug( "clear() ..." );
            }
        };
    }

    /**
     *
     */
    public static interface TouchEvent extends Event {
        @JSProperty
        int getPageX();

        @JSProperty
        int getPageY();

        @JSProperty
        int getDetail();

        @JSProperty
        TouchList getTouches();

//        void initMouseEvent(String type, boolean canBubble, boolean cancelable, JSObject view, int detail, int screenX,
//                int screenY, int clientX, int clientY, boolean ctrlKey, boolean altKey, boolean shiftKey, boolean metaKey,
//                short button, EventTarget relatedTarget);
    }


    public interface TouchList extends JSObject {
        @JSProperty
        int getLength();

        Touch item( int index );
    }


    public interface Touch extends JSObject {
        @JSProperty
        int getScreenX();

        @JSProperty
        int getScreenY();

        @JSProperty
        int getClientX();

        @JSProperty
        int getClientY();
    }


    protected static class ListenerHandleImpl
            extends ListenerHandle {

        public EventType        type;

        public EventListener<?> listener;

        protected ListenerHandleImpl( EventType type, EventListener<?> listener ) {
            this.type = type;
            this.listener = listener;
        }
    }

}
