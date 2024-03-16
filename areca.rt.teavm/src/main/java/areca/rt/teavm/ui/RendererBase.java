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
package areca.rt.teavm.ui;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

import areca.common.Assert;
import areca.common.Platform;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.NoRuntimeInfo;
import areca.ui.Position;
import areca.ui.component2.Events;
import areca.ui.component2.Events.EventType;
import areca.ui.component2.UIComponent;

/**
 *
 * @author Falko Bräutigam
 */
//@RuntimeInfo
public class RendererBase {

    private static final Log LOG = LogFactory.getLog( RendererBase.class );

    private static HTMLDocument         doc = Window.current().getDocument();


    protected HTMLDocument doc() {
        return doc;
    }


    @SuppressWarnings("unchecked")
    protected <R extends HTMLElement> R htmlElm( UIComponent c ) {
        return Assert.notNull( (R)c.htmlElm );
    }


    @NoRuntimeInfo
    protected void propagateEvent( UIComponent c, Event htmlEv, EventType type ) {
        if (htmlEv != null) {
            htmlEv.stopPropagation();
            htmlEv.preventDefault();
        }

        for (var handler : c.events) {
            if (handler.type == type) {
                try {
                    LOG.debug( "Handler: ..." );
                    var uiev = new Events.UIEvent( c, htmlEv, handler.type ) {
                        @Override public Position clientPos() {
                            return null;
                            //return Position.of( ((MouseEvent)_htmlEv).getClientX(), ((MouseEvent)_htmlEv).getClientY() );
                        }
                    };
                    handler.consumer.accept( uiev );
                }
                catch (Exception e) {
                    Throwable rootCause = Platform.rootCause( e );
                    LOG.info( "Root cause: %s : %s", rootCause, rootCause.getMessage() );
                    throw (RuntimeException)e; // help TeaVM to print proper stack
                }
            }
        }
    }

}
