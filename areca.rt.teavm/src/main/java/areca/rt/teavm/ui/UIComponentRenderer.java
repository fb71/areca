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
import areca.ui.Property;
import areca.ui.UIComponent;
import areca.ui.UIRenderEvent;
import areca.ui.UIRenderEvent.ComponentCreatedEvent;
import areca.ui.UIRenderEvent.ComponentDestroyedEvent;

/**
 *
 * @author falko
 */
public abstract class UIComponentRenderer<C extends UIComponent>
        extends UIRenderer {

    private static final Logger LOG = Logger.getLogger( UIComponentRenderer.class.getSimpleName() );

    public static final String      DATA_ELM = "_html_elm_";

    private Class<C>                componentType;



    protected UIComponentRenderer( Class<C> componentType ) {
        this.componentType = componentType;
    }


    @Override
    @SuppressWarnings("unchecked")
    public void handle( UIRenderEvent ev ) {
        super.handle( ev );

        if (ev instanceof Property.PropertyChangedEvent) {
            UIComponent component = ((Property)ev.getSource()).component();
            if (componentType.isAssignableFrom( component.getClass() )) {
                handlePropertyChanged( (Property.PropertyChangedEvent)ev, (C)component );
            }
        }
        else {
            UIComponent component = (UIComponent)ev.getSource();
            if (componentType.isAssignableFrom( component.getClass() )) {
                if (ev instanceof UIRenderEvent.ComponentCreatedEvent) {
                    handleComponentCreated( (ComponentCreatedEvent)ev, (C)component );
                }
                else if (ev instanceof UIRenderEvent.ComponentDestroyedEvent) {
                    handleComponentDestroyed( (ComponentDestroyedEvent)ev, (C)component );
                }
                else {
                    throw new IllegalStateException( "Unhandled event type: " +  ev );
                }
            }
        }
    }


    protected void handleComponentCreated( UIRenderEvent.ComponentCreatedEvent ev, C component ) {
        Assert.<HTMLElement>notNull( htmlElementOf( component ), "Call super.handleComponentCreated() *after* HTML-element was created." )
                .getStyle().setProperty( "position", "absolute" );
    }


    protected void handlePropertyChanged( Property.PropertyChangedEvent ev, C component ) {
        HTMLElement elm = htmlElementOf( component );
        elm.setAttribute( "id", String.valueOf( component.id() ) );

        // 'class' attribute
        StringBuilder classes = new StringBuilder( 128 );
        for (Class cl=component.getClass(); !cl.equals( Object.class ); cl=cl.getSuperclass()) {
            classes.append( classes.length() > 0 ? " " : "" ).append( cl.getSimpleName() );
        }
        elm.setAttribute( "class", classes.toString() );

        // background-color
        if (UIComponent.TYPE.bgColor.equals( ev.getSource() )) {
            LOG.info( "Color: " + ev.getNewValue() );
            elm.getStyle().setProperty( "background-color", ev.<Color>optNewValue().map(c -> c.toHex() ).orElse( null ) );
        }
    }


    protected void handleComponentDestroyed( UIRenderEvent.ComponentDestroyedEvent ev, C component ) {
        throw new RuntimeException( "not yet implemented." );
    }


    public static <R extends HTMLElement> R htmlElementOf( UIComponent component ) {
        return component.<R>optData( DATA_ELM ).orElseThrow( () -> new IllegalStateException( "No HTML element found." ) );
    }

}