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

import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.EventTarget;
import org.teavm.jso.dom.events.MouseEvent;

import areca.common.base.Consumer;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.html.HtmlEventTarget;
import areca.ui.html.HtmlEventTarget.HtmlEventListeners;
import areca.ui.html.HtmlEventTarget.HtmlMouseEvent;
import areca.ui.html.HtmlEventTarget.ListenerHandle;

/**
 *
 * @author Falko Bräutigam
 */
public class HtmlEventTargetImpl {

    private static final Log log = LogFactory.getLog( HtmlEventTargetImpl.class );


    public static void init( HtmlEventTarget elm, EventTarget delegate ) {

        elm.listeners = new HtmlEventListeners() {
            @Override
            public ListenerHandle click( Consumer<HtmlMouseEvent,RuntimeException> handler ) {
                // EventListener<MouseEvent> listener = ev -> handler.accept( HtmlMouseEventImpl.create( (MouseEvent)ev ) );
                return add( "click", ev -> handler.accept( HtmlMouseEventImpl.create( (MouseEvent)ev ) ) );
            }
            @Override
            public ListenerHandle mouseMove( Consumer<HtmlMouseEvent,RuntimeException> handler ) {
                return add( "mousemove", ev -> handler.accept( HtmlMouseEventImpl.create( (MouseEvent)ev ) ) );
            }
            public ListenerHandle add( String type, EventListener<?> listener ) {
                delegate.addEventListener( type, listener );
                return new ListenerHandleImpl( type, listener );
            }
            @Override
            public void remove( ListenerHandle handle ) {
                delegate.removeEventListener( ((ListenerHandleImpl)handle).type, ((ListenerHandleImpl)handle).listener );
            }
        };
    }


    protected static class ListenerHandleImpl
            extends ListenerHandle {

        public String           type;

        public EventListener<?> listener;

        protected ListenerHandleImpl( String type, EventListener<?> listener ) {
            this.type = type;
            this.listener = listener;
        }
    }

}
