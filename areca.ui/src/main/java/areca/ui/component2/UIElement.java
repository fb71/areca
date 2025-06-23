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

import areca.common.Session;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.Property.PropertyContainer;
import areca.ui.component2.UIComponentEvent.ComponentDisposedEvent;
import areca.ui.component2.UIComponentEvent.ComponentConstructingEvent;

/**
 *
 * @author Falko Bräutigam
 */
public abstract class UIElement
        implements PropertyContainer {

    private static final Log LOG = LogFactory.getLog( UIElement.class );

    /**
     * Provides unique (global) component IDs.
     * XXX Seems to overflow on Server Platform!?
     */
    private static class GlobalUniqueId {
        static volatile int count = 0;

        public static int next() {
            return count++;
        }
    }

    /**
     * Provides unique (per {@link Session}) component IDs.
     */
    private static class SessionUniqueId {
        int count = 0;

        public static int next() {
            return Session.instanceOf( SessionUniqueId.class ).count++;
        }

        static {
            Session.registerFactory( SessionUniqueId.class, SessionUniqueId::new );
        }
    }

    // instance *******************************************

    private int                         id = SessionUniqueId.next();

    private boolean                     disposed;

    private Map<String,Property<?,?>>   properties = new HashMap<>( 32 );

    /**
     * Init
     */
    {
        UIComponentEvent.manager().publish( new ComponentConstructingEvent( this ) );
    }


    /**
     * Destruct this element.
     */
    public void dispose() {
        if (!isDisposed()) {
            disposed = true;
            UIComponentEvent.manager().publish( new ComponentDisposedEvent( this ) );
        }
        else {
            LOG.info( "DISPOSE: already disposed! (%s)", getClass().getName() );
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
