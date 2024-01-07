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

import org.teavm.jso.JSBody;
import org.teavm.jso.dom.events.MouseEvent;
import org.teavm.jso.dom.html.HTMLElement;

import areca.common.Assert;
import areca.common.Platform;
import areca.common.base.Sequence;
import areca.common.event.EventHandler;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.ui.Align.Vertical;
import areca.ui.Position;
import areca.ui.component2.Events.UIEvent;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComponentEvent;
import areca.ui.component2.UIComponentEvent.ComponentAttachedEvent;
import areca.ui.component2.UIComponentEvent.ComponentConstructedEvent;
import areca.ui.component2.UIComponentEvent.ComponentConstructingEvent;
import areca.ui.component2.UIComponentEvent.ComponentDetachedEvent;
import areca.ui.component2.UIComponentEvent.ComponentDisposedEvent;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class UIComponentRenderer
        extends RendererBase {

    private static final Log LOG = LogFactory.getLog( UIComponentRenderer.class );

    public static final ClassInfo<UIComponentRenderer> TYPE = UIComponentRendererClassInfo.instance();

    /**
     * Starts all component renderes of this package.
     */
    public static void start() {
        UICompositeRenderer._start();
        ScrollableCompositeRenderer._start(); // after UIComposite
        TextRenderer._start();
        TextFieldRenderer._start();
        ButtonRenderer._start();
        ProgressRenderer._start();
        LinkRenderer._start();
        SeparatorRenderer._start();

        BadgeRenderer._start();
        LabelRenderer._start();
        TagRenderer._start();
        UIComponentRenderer._start(); // last
    }

    static void _start() {
        UIComponentEvent.manager()
                .subscribe( new UIComponentRenderer() )
                .performIf( ev -> ev instanceof UIComponentEvent && ev.getSource() instanceof UIComponent );
    }

    // instance *******************************************

    protected HTMLElement htmlElm( UIComponent c ) {
        return Assert.notNull( (HTMLElement)c.htmlElm, "No htmlElm for: " + c.getClass().getSimpleName() );
    }


    @EventHandler( ComponentConstructingEvent.class )
    public void componentConstructing( ComponentConstructingEvent ev ) {
    }


    @EventHandler( ComponentConstructedEvent.class )
    public void componentConstructed( ComponentConstructedEvent ev ) {
        UIComponent c = ev.getSource();
        LOG.debug( "CONSTRUCTED: " + c.getClass().getName() );
        HTMLElement htmlElm = htmlElm( c );

        c.tooltip
                .onInitAndChange( (newValue, __) -> {
                    htmlElm.setAttribute( "title", newValue );
                });

        // cssClasses
        c.cssClasses
                .onInitAndChange( (newValue, oldValue) -> {
                    Assert.notNull( newValue, "Setting null value means remove() ???" );
                    htmlElm.setAttribute( "class", String.join( " ", newValue ) );
                });

        // cssStyles
        c.styles
                .onInitAndChange( (newValue, oldValue) -> {
                    Assert.notNull( newValue, "Setting null value means remove() ???" );
                    LOG.debug( "CSS: %s", newValue );
                    // new/updated
                    newValue.forEach( elm -> htmlElm.getStyle().setProperty( elm.name, elm.value ) );
                    // removed
                    if (oldValue != null) {
                        Sequence.of( oldValue )
                                .filter( elm -> !newValue.contains( elm ) )
                                .forEach( elm -> htmlElm.getStyle().removeProperty( elm.name ) );
                    }
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

        // bgImage
        c.bgImage.onInitAndChange( (newValue,oldValue) -> {
            if (newValue != null) {
                var dataUrl = String.format( "url(data:;base64,%s)", newValue.replace( "\r", "" ).replace( "\n", "" ) );
                htmlElm.getStyle().setProperty( "background-image", dataUrl ); // position, repeat, size in CSS
//                htmlElm.getStyle().setProperty( "background-position", "center" );
//                htmlElm.getStyle().setProperty( "background-size", "100%" );
//                htmlElm.getStyle().setProperty( "background-repeat", "no-repeat" );
                //htmlElm.getStyle().setProperty( "background", String.format( "url(data:;base64,%s) no-repeat center", newValue ) );
            } else {
                htmlElm.getStyle().removeProperty( "background-image" );
            }
        });

        // opacity
        c.opacity
                .onInitAndChange( (newValue, oldValue) -> {
                    if (newValue == null) {
                        htmlElm.getStyle().removeProperty( "opacity" );
                    } else {
                        htmlElm.getStyle().setProperty( "opacity", newValue.toString() );
                    }
                });

        // enabled
        c.enabled
                .onInitAndChange( (newValue, oldValue) -> {
                    if (newValue == null || newValue) {
                        htmlElm.removeAttribute( "disabled" );
                    } else {
                        htmlElm.setAttribute( "disabled", "disabled" );
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

        // minimumHeight
        // EXPERIMENTAL: see UIComponent#minimumHeight
        c.minimumHeight = width -> {
            htmlElm.getStyle().removeProperty( "height" );
            htmlElm.getStyle().setProperty( "width", String.format( "%spx", width ) );
            return htmlElm.getOffsetHeight();
        };

        // position
        c.position
                .onInitAndChange( (newValue, oldValue) -> {
                    if (newValue == null) {
                        htmlElm.getStyle().removeProperty( "left" );
                        htmlElm.getStyle().removeProperty( "top" );
                    }
                    else {
                        htmlElm.getStyle().setProperty( "left", String.format( "%spx", newValue.x() ) );
                        htmlElm.getStyle().setProperty( "top", String.format( "%spx", newValue.y() ) );
                    }
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
                    ((MouseEvent)_htmlEv).stopPropagation();
                    ((MouseEvent)_htmlEv).preventDefault();
                    try {
                        handler.consumer.accept( new UIEvent( c, _htmlEv, handler.type ) {
                            @Override
                            public Position clientPos() {
                                return Position.of( ((MouseEvent)_htmlEv).getClientX(), ((MouseEvent)_htmlEv).getClientY() );
                            }
                        });
                    }
                    catch (Exception e) {
                        Throwable rootCause = Platform.rootCause( e );
                        LOG.info( "Root cause: %s : %s", rootCause, rootCause.getMessage() );
                        throw (RuntimeException)e; // help TeaVM to print proper stack
                    }
                });
            }
        });

        // scrollIntoView
        c.scrollIntoView.onInitAndChange( (newValue, __) -> {
            LOG.debug( "Scroll: %s", c );
            ((ScrollableHTMLElement)htmlElm).scrollIntoView( "smooth", newValue == Vertical.TOP ? "start" : "end" );
        });

        // focus
        c.focus.onInitAndChange( (newValue, __) -> {
            if (newValue) {
                htmlElm.focus();
            } else {
                htmlElm.blur();
            }
        });
    }


    public interface ScrollableHTMLElement
            extends HTMLElement {
        @JSBody(params = {"behavior", "block"}, script = "this.scrollIntoView({'block':block, 'behavior':behavior});")
        void scrollIntoView( String behavior, String block );
    }


    @EventHandler( ComponentAttachedEvent.class )
    public void componentAttached( ComponentAttachedEvent ev ) {
        LOG.debug( "ATTACHED: " + ev.getSource().getClass().getSimpleName() );
        if (ev.getSource().parent() != ev.parent) {
            LOG.info( "ATTACHED: parent already changed!" );
        }
        var htmlParent = htmlElm( ev.parent );
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
