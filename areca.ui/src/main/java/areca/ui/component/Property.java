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

import areca.common.Assert;
import areca.common.base.Consumer;
import areca.common.base.Opt;
import areca.common.base.Sequence;
import areca.common.base.Supplier;
import areca.common.event.EventManager;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author falko
 */
public abstract class Property<T> {

    private static final Log LOG = LogFactory.getLog( Property.class );

    /**
     * Creates a field backed read-write property.
     */
    public static <R> ReadWrite<R> create( Object component, String name ) {
        return new FieldBackedProperty<>( component, name, null );
    }

    /**
     * Creates a field backed read-write property with the given initial value.
     */
    public static <R> ReadWrite<R> create( Object component, String name, R initialValue ) {
        return new FieldBackedProperty<>( component, name, initialValue );
    }

    /**
     *
     */
    public static <R> ReadOnly<R> create( Object component, String name, Supplier.$<R> getter ) {
        return new ReadOnly<>( component, name ) {
            @Override protected R doGet() {
                return getter.get();
            }
        };
    }

    /**
     *
     */
    public static <R> ReadWrite<R> create( Object component, String name, Supplier.$<R> getter, Consumer.$<R> setter ) {
        return new ReadWrite<>( component, name ) {
            @Override protected R doGet() {
                return getter.get();
            }
            @Override protected void doSet( R newValue ) {
                LOG.info( "%s.%s = %s", component.getClass().getSimpleName(), name, newValue );
                setter.accept( newValue );
            }
        };
    }

    // instance *******************************************

    protected Object component;

    protected String name;


    protected Property( Object component, String name ) {
        this.component = component;
        this.name = name;
    }

    @Override
    public String toString() {
        return String.format( "Property[%s]", name );
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
       // result = prime * result + ((component == null) ? 0 : component.getClass().getName().hashCode());
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

    public String name() {
        return name;
    }

    protected void fireEvent( Object oldValue, Object newValue ) {
        EventManager.instance().publish( new PropertyChangedEvent( this, oldValue, newValue ) );
    }


    /**
     *
     */
    public static abstract class ReadOnly<T>
            extends Property<T> {

        protected ReadOnly( Object component, String name ) {
            super( component, name );
        }

        protected abstract T doGet();

        public T get() {
            return Assert.notNull( doGet(), "Property '" + name + "' is null." );
        }

        public Opt<T> opt() {
            return Opt.ofNullable( doGet() );
        }
    }


    /**
     *
     */
    public static abstract class ReadWrite<T>
            extends ReadOnly<T> {

        protected ReadWrite( Object component, String name ) {
            super( component, name );
        }

        protected abstract void doSet( T newValue );

        /** Set value without firing {@link PropertyChangedEvent}. */
        public ReadWrite<T> rawSet( T newValue ) {
            doSet( newValue );
            return this;
        }

        public ReadWrite<T> set( T newValue ) {
            doSet( newValue );
            return this;
        }

//        @SuppressWarnings("unchecked")
//        public <TT extends T,E extends Exception> Property<T> modify( Consumer<TT,E> modifier ) throws E {
//            modifier.accept( (TT)value );
//            EventManager.instance().publish( new PropertyChangedEvent( this, value, value ) );
//            return this;
//        }
    }


    /**
     *
     */
    public static class FieldBackedProperty<T>
            extends ReadWrite<T> {

        protected T value;

        protected FieldBackedProperty( Object component, String name, T initialValue ) {
            super( component, name );
            this.value = initialValue;
        }

        @Override
        protected void doSet( T newValue ) {
            value = newValue;
        }

        @Override
        protected T doGet() {
            return value;
        }


//        @SuppressWarnings("unchecked")
//        public <TT extends T,E extends Exception> Property<T> modify( Consumer<TT,E> modifier ) throws E {
//            modifier.accept( (TT)value );
//            EventManager.instance().publish( new PropertyChangedEvent( this, value, value ) );
//            return this;
//        }
    }


    /**
     *
     */
    public static abstract class ReadOnlys<T>
            extends Property<T> {

        protected ReadOnlys( Object component, String name ) {
            super( component, name );
        }

        public abstract Sequence<T,RuntimeException> sequence();
    }


    /**
     *
     */
    public static abstract class ReadWrites<T>
            extends ReadOnlys<T> {

        protected ReadWrites( Object component, String name ) {
            super( component, name );
        }

        protected abstract void doAdd( T value );

        protected abstract void doRemove( T value );

        public <R extends T> R add( R value ) {
            doAdd( value );
            return value;
        }

        protected void remove( T value ) {
            doRemove( value );
        }
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
