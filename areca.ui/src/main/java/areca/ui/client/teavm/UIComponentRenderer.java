/*
 * polymap.org
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
package areca.ui.client.teavm;

import java.util.logging.Logger;

import org.teavm.jso.dom.html.HTMLElement;

import areca.ui.Color;
import areca.ui.Property;
import areca.ui.UIComponent;
import areca.ui.UIRenderEvent;
import areca.ui.UIRenderEvent.ComponentCreated;
import areca.ui.UIRenderEvent.ComponentDestroyed;

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
    public void handle( UIRenderEvent ev ) {
        super.handle( ev );

        if (ev instanceof Property.PropertySetEvent) {
            UIComponent component = ((Property)ev.getSource()).component();
            if (componentType.isAssignableFrom( component.getClass() )) {
                handlePropertyChange( (Property.PropertySetEvent)ev, (C)component );
            }
        }
        else if (ev instanceof UIRenderEvent.ComponentCreated) {
            UIComponent component = ((ComponentCreated)ev).getSource();
            if (componentType.isAssignableFrom( component.getClass() )) {
                handleComponentCreated( (ComponentCreated)ev, (C)component );
            }
        }
        else if (ev instanceof UIRenderEvent.ComponentDestroyed) {
            UIComponent component = ((ComponentDestroyed)ev).getSource();
            if (componentType.isAssignableFrom( component.getClass() )) {
                handleComponentDestroyed( (ComponentDestroyed)ev, (C)component );
            }
        }
        else {
            throw new RuntimeException( "Unhandled event type: " +  ev );
        }
    }


    protected void handleComponentCreated( UIRenderEvent.ComponentCreated ev, C component ) {
        assert htmlElementOf( component ) != null : "Call super.handleComponentCreated() *after* HTML-element was created.";
        htmlElementOf( component ).getStyle().setProperty( "position", "absolute" );
    }


    protected void handlePropertyChange( Property.PropertySetEvent ev, C component ) {
        HTMLElement elm = htmlElementOf( component );
        elm.setAttribute( "id", String.valueOf( component.id() ) );

        StringBuilder classes = new StringBuilder( 128 );
        for (Class cl=component.getClass(); !cl.equals( Object.class ); cl=cl.getSuperclass()) {
            classes.append( classes.length() > 0 ? " " : "" ).append( cl.getSimpleName() );
        }
        elm.setAttribute( "class", classes.toString() );

        if (UIComponent.TYPE.bgColor.equals( ev.getSource() )) {
            LOG.info( "Color: " + ev.getNewValue() );
            elm.getStyle().setProperty( "background-color", ev.<Color>optNewValue().map(c -> c.toHex() ).orElse( null ) );
        }
    }


    protected void handleComponentDestroyed( UIRenderEvent.ComponentDestroyed ev, C component ) {
        throw new RuntimeException( "not yet implemented." );
    }


    public static <R extends HTMLElement> R htmlElementOf( UIComponent component ) {
        return component.data( DATA_ELM );
    }

}