/*
 * Copyright (C) 2024, the @authors. All rights reserved.
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
package areca.ui.component2;

import java.util.HashMap;
import java.util.Map;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.Property.PropertyContainer;

/**
 *
 * @author Falko Bräutigam
 */
public abstract class UIElement
        implements PropertyContainer {

    private static final Log LOG = LogFactory.getLog( UIElement.class );

    private static volatile int         ID_COUNT;

    private int                         id = ID_COUNT++;

    private boolean                     disposed;

    private Map<String,Property<?,?>>   properties = new HashMap<>( 32 );

    /**
     * Init
     */
    {
        UIComponentEvent.manager().publish( new UIComponentEvent.ComponentConstructingEvent( this ) );
    }


    protected UIElement() {
        UIComponentEvent.manager().publish( new UIComponentEvent.ComponentConstructedEvent( this ) );
    }


    /**
     * Destruct this element.
     */
    public void dispose() {
        if (isDisposed()) {
            LOG.info( "DISPOSE: already disposed! (%s)", getClass().getName() );
        }
        else {
            disposed = true;
            UIComponentEvent.manager().publish( new UIComponentEvent.ComponentDisposedEvent( this ) );
        }
    }


    public boolean isDisposed() {
        return disposed;
    }


    public int id() {
        return id;
    }


    /**
     * The unique ID of a component is automatically set by the framework.
     * Change it only if you really have to and you know what you are doing.
     */
    public UIElement setId( int newId ) {
        this.id = newId;
        return this;
    }

    // PropertyContainer **********************************

    @Override
    public void registerProperty( Property<?,?> prop ) {
        properties.put( prop.name(), prop );
    }


    @Override
    public Iterable<Property<?,?>> properties() {
        return properties.values();
    }


    public Property<?,?> propertyForName( String name ) {
        return properties.get( name );
    }
}
