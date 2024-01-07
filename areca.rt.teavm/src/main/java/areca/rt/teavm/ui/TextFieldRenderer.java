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

import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.html.HTMLInputElement;
import org.teavm.jso.dom.html.HTMLTextAreaElement;

import areca.common.Platform;
import areca.common.event.EventHandler;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.ui.Position;
import areca.ui.component2.Events;
import areca.ui.component2.Events.EventType;
import areca.ui.component2.TextField;
import areca.ui.component2.UIComponentEvent;
import areca.ui.component2.UIComponentEvent.ComponentConstructedEvent;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class TextFieldRenderer
        extends RendererBase {

    private static final Log LOG = LogFactory.getLog( TextFieldRenderer.class );

    public static final ClassInfo<TextFieldRenderer> TYPE = TextFieldRendererClassInfo.instance();

    static void _start() {
        UIComponentEvent.manager()
                .subscribe( new TextFieldRenderer() )
                .performIf( ev -> ev instanceof ComponentConstructedEvent && ev.getSource() instanceof TextField );
    }

    // instance *******************************************

    @EventHandler( ComponentConstructedEvent.class )
    public void componentConstructed( ComponentConstructedEvent ev ) {
        TextField c = (TextField)ev.getSource();

        // multiline textarea
        if (c.multiline.get()) {
            var textarea = (HTMLTextAreaElement)(c.htmlElm = doc().createElement( "textarea" ));

            c.content.onInitAndChange( (newValue, oldValue) -> {
                LOG.debug( "Set: %s", newValue );
                textarea.setValue( newValue != null ? newValue : "" );
            });
            textarea.addEventListener( "input", htmlEv -> {
                htmlEv.stopPropagation();
                htmlEv.preventDefault();

                c.content.rawSet( textarea.getValue() );
                LOG.debug( "Text: %s", textarea.getValue() );
                propagateEvent( c, htmlEv );
            });
        }
        // input
        else {
            var input = (HTMLInputElement)(c.htmlElm = doc().createElement( "input" ));
            input.setAttribute( "type", "text" );

            c.content.onInitAndChange( (newValue, oldValue) -> {
                LOG.debug( "Set: %s", newValue );
                input.setValue( newValue != null ? newValue : "" );
            });

            LOG.info( "Register listener: %s", "input" );
            input.addEventListener( "input", htmlEv -> {
                htmlEv.stopPropagation();
                htmlEv.preventDefault();

                c.content.rawSet( input.getValue() );
                LOG.debug( "Text: %s", input.getValue() );
                propagateEvent( c, htmlEv );
            });
        }
    }


    protected void propagateEvent( TextField c, Event htmlEv ) {
        for (var handler : c.events) {
            if (handler.type == EventType.TEXT) {
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
