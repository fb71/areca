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
import org.teavm.jso.dom.events.MouseEvent;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

import areca.common.Assert;
import areca.common.event.EventHandler;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.RuntimeInfo;
import areca.ui.Position;
import areca.ui.component2.Events.UIEvent;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComponentEvent.ComponentConstructedEvent;
import areca.ui.component2.UIComponentEvent.ComponentDetachedEvent;
import areca.ui.component2.UIComponentEvent.ComponentDisposedEvent;
import areca.ui.component2.UIComponentEvent.ComponentAttachedEvent;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public abstract class UIComponentRenderer {

    private static final Log LOG = LogFactory.getLog( UIComponentRenderer.class );

    private static HTMLDocument         doc = Window.current().getDocument();

    public static void start() {
        UICompositeRenderer._start();
        TextRenderer._start();
        ButtonRenderer._start();
    }

    // instance *******************************************

    protected HTMLDocument doc() {
        return doc;
    }


    protected HTMLElement htmlElm( UIComponent c ) {
        return Assert.notNull( (HTMLElement)c.htmlElm );
    }


    @EventHandler( ComponentConstructedEvent.class )
    public void componentConstructed( ComponentConstructedEvent ev ) {
        UIComponent c = ev.getSource();
        LOG.debug( "CONSTRUCTED: " + c.getClass().getName() );
        HTMLElement htmlElm = htmlElm( c );

        // cssClasses
        c.cssClasses
                .onInitAndChange( (newValue, oldValue) -> {
                    Assert.notNull( newValue, "Setting null value means remove() ???" );
                    htmlElm.setAttribute( "class", String.join( " ", newValue ) );
                });

        // bgColor
        c.bgColor
                .onInitAndChange( (newValue, oldValue) -> {
                    Assert.notNull( newValue, "Setting null value means remove() ???" );
                    htmlElm.getStyle().setProperty( "background-color", newValue.toHex() );
                });
                // .defaultsTo( () -> {
                //    return Color.ofHex( htmlElm.getStyle().getPropertyValue( "background-color" ) );
                //});

        // opacity
        c.opacity
                .onInitAndChange( (newValue, oldValue) -> {
                    if (newValue == null) {
                        htmlElm.getStyle().removeProperty( "opacity" );
                    } else {
                        htmlElm.getStyle().setProperty( "opacity", newValue.toString() );
                    }
                });

        // size
        c.size
                .onInitAndChange( (newValue, oldValue) -> {
                    Assert.notNull( newValue, "Setting null value means remove() ???" );
                    htmlElm.getStyle().setProperty( "width", String.format( "%spx", newValue.width() ) );
                    htmlElm.getStyle().setProperty( "height", String.format( "%spx", newValue.height() ) );
                });
//                .defaultsTo( () -> {
//                    return Size.of( htmlElm.getOffsetWidth(), htmlElm.getOffsetHeight() );
//                });

        // position
        c.position
                .onInitAndChange( (newValue, oldValue) -> {
                    Assert.notNull( newValue, "Setting null value means remove() ???" );
                    htmlElm.getStyle().setProperty( "left", String.format( "%spx", newValue.x() ) );
                    htmlElm.getStyle().setProperty( "top", String.format( "%spx", newValue.y() ) );
                });
                //.initWith( () -> {
                //    return Position.of( htmlElm.getOffsetLeft(), htmlElm.getOffsetTop() );
                //});

        // events
        c.events.onInitAndChange( (newValue, oldValue) -> {
            for (areca.ui.component2.Events.EventHandler handler : newValue) {
                // FIXME check
                if (oldValue != null && oldValue.contains( handler )) {
                    continue;
                }
                LOG.debug( "ADDING: " + newValue + " / " + oldValue );

                String type = null;
                switch (handler.type) {
                    case SELECT: type = "click"; break;
                    case ACTION: type = "dblclick"; break;
                    case CONTEXT: type = "contextmenu"; break;
                    default: type = handler.type.toString().toLowerCase();
                }
                htmlElm( c ).addEventListener( type, _htmlEv -> {
                    LOG.debug( "HTML event: " + handler + " " + ((MouseEvent)_htmlEv).getButton() );
                    //((MouseEvent)_htmlEv).stopPropagation();
                    handler.consumer.accept( new UIEvent( c ) {
                        {
                            this.htmlEv = _htmlEv;
                            this.type = handler.type;
                        }
                        @Override
                        public Position clientPos() {
                            return Position.of( ((MouseEvent)_htmlEv).getClientX(), ((MouseEvent)_htmlEv).getClientY() );
                        }});
                });
            }
        });
    }


    @EventHandler( ComponentAttachedEvent.class )
    public void componentAttached( ComponentAttachedEvent ev ) {
        LOG.debug( "ATTACHED: " + ev.getSource().getClass().getSimpleName() );
        var htmlParent = htmlElm( ev.getSource().parent() );
        htmlParent.appendChild( htmlElm( ev.getSource() ) );
    }


    @EventHandler( ComponentDetachedEvent.class )
    public void componentDetached( ComponentDetachedEvent ev ) {
        LOG.debug( "DETACHED: " + ev.getSource().getClass().getSimpleName() );
        var htmlElm = htmlElm( ev.getSource() );
        htmlElm.getParentNode().removeChild( htmlElm );
    }


    @EventHandler( ComponentDisposedEvent.class )
    public void componentDisposed( ComponentDisposedEvent ev ) {
        LOG.debug( "DISPOSED: " + ev.getSource().getClass().getName() );
        var htmlElm = htmlElm( ev.getSource() );
        Assert.isNull( htmlElm.getParentNode() );
    }

}
