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
package areca.ui.component;

import java.util.logging.Logger;

import areca.common.Assert;
import areca.common.base.Consumer;
import areca.common.base.Opt;
import areca.common.event.EventManager;

/**
 *
 * @author falko
 */
public class Property<T> {

    private static final Logger LOG = Logger.getLogger( Property.class.getSimpleName() );

    public static <R> Property<R> create( Object component, String name ) {
        return new Property<>( component, name, null );
    }

    public static <R> Property<R> create( Object component, String name, R initialValue ) {
        return new Property<>( component, name, initialValue );
    }


    // instance *******************************************

    private Object      component;

    private String      name;

    private T           value;


    protected Property( Object component, String name, T value ) {
        this.component = component;
        this.name = name;
        this.value = value;
    }


    @Override
    public String toString() {
        return "Property[" + component.getClass().getSimpleName() + "." + name + "]";
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((component == null) ? 0 : component.getClass().getName().hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }


    @Override
    public boolean equals( Object obj ) {
        if (obj instanceof Property) {
            Property<?> other = (Property<?>)obj;
            return name.equals( other.name ); // &&
//                    // XXX we don't know to correct Type the Property is a Field of
//                    (component.getClass().isAssignableFrom( other.component.getClass() )
//                    || other.component.getClass().isAssignableFrom( component.getClass() ));

        }
        return false;
    }


    public Object component() {
        return component;
    }


    public String name() {
        return name;
    }


    /** Set value without firing {@link PropertyChangedEvent}. */
    public Property<T> rawSet( T newValue ) {
        this.value = newValue;
        return this;
    }


    public Property<T> set( T newValue ) {
        T oldValue = value;
        this.value = newValue;
        EventManager.instance().publish( new PropertyChangedEvent( this, oldValue, newValue ) );
        return this;
    }


    @SuppressWarnings("unchecked")
    public <TT extends T,E extends Exception> Property<T> modify( Consumer<TT,E> modifier ) throws E {
        modifier.accept( (TT)value );
        EventManager.instance().publish( new PropertyChangedEvent( this, value, value ) );
        return this;
    }


    public T get() {
        Assert.notNull( value );
        return value;
    }


    public Opt<T> opt() {
        return Opt.ofNullable( value );
    }


    /**
     *
     */
    public static class PropertyChangedEvent
            extends UIRenderEvent {

        private Object oldValue;

        private Object newValue;

        protected PropertyChangedEvent( Property<?> source, Object oldValue, Object newValue ) {
            super( source );
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        @Override
        public String toString() {
            return "PropertyChangedEvent[source=" + getSource() + ", newValue=" + newValue + "]";
        }

        @Override
        public Property<?> getSource() {
            return (Property<?>)super.getSource();
        }

        @SuppressWarnings("unchecked")
        public <R> R getOldValue() {
            return (R)oldValue;
        }

        @SuppressWarnings("unchecked")
        public <R> Opt<R> optOldValue() {
            return Opt.ofNullable( (R)oldValue );
        }

        @SuppressWarnings("unchecked")
        public <R> R getNewValue() {
            return (R)newValue;
        }

        @SuppressWarnings("unchecked")
        public <R> Opt<R> optNewValue() {
            return Opt.ofNullable( (R)newValue );
        }
    }

}
