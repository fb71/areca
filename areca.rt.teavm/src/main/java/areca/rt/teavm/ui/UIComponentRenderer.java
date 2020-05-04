/*
 * Copyright (C) 2019, the @authors. All rights reserved.
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

import java.util.logging.Logger;

import org.teavm.jso.dom.html.HTMLElement;

import areca.common.Assert;
import areca.ui.Color;
import areca.ui.Position;
import areca.ui.Size;
import areca.ui.component.Property;
import areca.ui.component.Property.PropertyChangedEvent;
import areca.ui.component.UIComponent;
import areca.ui.component.UIRenderEvent;
import areca.ui.component.UIRenderEvent.ComponentCreatedEvent;
import areca.ui.component.UIRenderEvent.ComponentDisposedEvent;

/**
 *
 * @author falko
 */
public abstract class UIComponentRenderer<C extends UIComponent,H extends HTMLElement>
        extends UIRenderer {

    private static final Logger LOG = Logger.getLogger( UIComponentRenderer.class.getSimpleName() );

    public static final String      DATA_ELM = "_html_elm_";

    private Class<C>                componentType;



    protected UIComponentRenderer( Class<C> componentType ) {
        this.componentType = componentType;
    }


    protected H handleComponentCreated( ComponentCreatedEvent ev, C component, H elm ) {
        elm.getStyle().setProperty( "position", "absolute" );
        elm.getStyle().setProperty( "transition", "top 0.5s, left 0.5s" );

        component.position.rawSet( Position.of( 0, 0 ) );
        Size size = Size.of( elm.getOffsetWidth(), elm.getOffsetHeight() );
        component.size.rawSet( size );
        component.minSize.rawSet( size );
        log( component, "INIT " + component.size.get() );
        return elm;
    }


    protected void handlePropertyChanged( PropertyChangedEvent ev, C component, H elm ) {
        elm.setAttribute( "id", String.valueOf( component.id() ) );

        // 'class' attribute
        StringBuilder classes = new StringBuilder( 128 );
        for (Class cl=component.getClass(); !cl.equals( Object.class ); cl=cl.getSuperclass()) {
            classes.append( classes.length() > 0 ? " " : "" ).append( cl.getSimpleName() );
        }
        elm.setAttribute( "class", classes.toString() );

        // background-color
        if (UIComponent.TYPE.bgColor.equals( ev.getSource() )) {
            log( component, "SET " + ev.getNewValue() );
            elm.getStyle().setProperty( "background-color", ev.<Color>optNewValue().transform( Color::toHex ).orElse( null ) );
        }
        // position
        if (UIComponent.TYPE.position.equals( ev.getSource() )) {
            log( component, "SET " + ev.getNewValue() );
            elm.getStyle().setProperty( "top", ev.<Position>optNewValue().get().y() + "px" );
            elm.getStyle().setProperty( "left", ev.<Position>optNewValue().get().x() + "px" );
        }
        // size
        if (UIComponent.TYPE.size.equals( ev.getSource() )) {
            log( component, "SET " + ev.getNewValue() );
            elm.getStyle().setProperty( "width", ev.<Size>optNewValue().get().width() + "px" );
            elm.getStyle().setProperty( "height", ev.<Size>optNewValue().get().height() + "px" );
        }
    }


    protected void handleComponentDestroyed( ComponentDisposedEvent ev, C component, H elm ) {
        throw new RuntimeException( "not yet implemented." );
    }


    @Override
    @SuppressWarnings("unchecked")
    public void handle( UIRenderEvent ev ) {
        super.handle( ev );

        // PropertyChangedEvent
        if (ev instanceof Property.PropertyChangedEvent) {
            UIComponent component = ((Property)ev.getSource()).component();
            if (componentType.isAssignableFrom( component.getClass() )) {
                handlePropertyChanged( (Property.PropertyChangedEvent)ev, (C)component, htmlElementOf( component ) );
            }
        }
        else {
            UIComponent component = (UIComponent)ev.getSource();
            if (componentType.isAssignableFrom( component.getClass() )) {
                // ComponentCreatedEvent
                if (ev instanceof UIRenderEvent.ComponentCreatedEvent) {
                    Assert.that( () -> !component.optData( DATA_ELM ).isPresent() );
                    component.data( DATA_ELM, () -> {
                        return handleComponentCreated( (ComponentCreatedEvent)ev, (C)component, null );
                    });
                    Assert.that( () -> component.optData( DATA_ELM ).isPresent() );
                }
                // ComponentDisposedEvent
                else if (ev instanceof UIRenderEvent.ComponentDisposedEvent) {
                    handleComponentDestroyed( (ComponentDisposedEvent)ev, (C)component, htmlElementOf( component ) );
                }
                else {
                    throw new IllegalStateException( "Unhandled event type: " +  ev );
                }
            }
        }
    }


    public static <R extends HTMLElement> R htmlElementOf( UIComponent component ) {
        return component.<R>optData( DATA_ELM ).orElseThrow( () -> new IllegalStateException( "No HTML element found." ) );
    }


    protected void log( C c, String msg ) {
        LOG.info( getClass().getSimpleName() + ": " + msg );
    }

}