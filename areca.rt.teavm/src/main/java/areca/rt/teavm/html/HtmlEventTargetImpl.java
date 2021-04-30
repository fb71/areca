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

import areca.common.base.Consumer.RConsumer;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
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
                    handler.accept( HtmlMouseEventImpl.create( (MouseEvent)ev ) );
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
                LOG.info( "clear() ..." );
            }
        };
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
