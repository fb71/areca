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
package areca.ui;

import java.util.EventObject;

import areca.common.Assert;
import areca.common.base.Consumer.RConsumer;
import areca.common.base.Opt;
import areca.common.base.Sequence;
import areca.common.base.Supplier.RSupplier;
import areca.common.event.EventManager;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
//import areca.ui.component.UIRenderEvent;

/**
 *
 * @param <T> The type of the value of this property.
 * @param <C> The type of the component this is a property of.
 * @author falko
 */
public abstract class Property<C,T> {

    private static final Log LOG = LogFactory.getLog( Property.class );

    /**
     * Creates a field backed read-write property.
     */
    public static <RC,R> ReadWrite<RC,R> create( RC component, String name ) {
        return new FieldBackedProperty<>( component, name, null );
    }

    /**
     * Creates a field backed read-write property with the given initial value.
     */
    public static <RC,R> ReadWrite<RC,R> create( RC component, String name, R initialValue ) {
        return new FieldBackedProperty<>( component, name, initialValue );
    }

    /**
     *
     */
    public static <RC,R> ReadOnly<RC,R> create( RC component, String name, RSupplier<R> getter ) {
        return new ReadOnly<>( component, name ) {
            @Override protected R doGet() {
                return getter.get();
            }
        };
    }

    /**
     *
     */
    public static <RC,R> ReadWrite<RC,R> create( RC component, String name, RSupplier<R> getter, RConsumer<R> setter ) {
        return new ReadWrite<>( component, name ) {
            @Override protected R doGet() {
                return getter.get();
            }
            @Override protected void doSet( R newValue ) {
                // LOG.debug( "%s.%s = %s", component.getClass().getSimpleName(), name, newValue );
                setter.accept( newValue );
            }
        };
    }

    // instance *******************************************

    protected C         component;

    protected String    name;


    protected Property( C component, String name ) {
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
            Property<?,?> other = (Property<?,?>)obj;
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
    public static abstract class ReadOnly<C,T>
            extends Property<C,T> {

        protected ReadOnly( C component, String name ) {
            super( component, name );
        }

        protected abstract T doGet();

        public T get() {
            return Assert.notNull( doGet(), "Property '" + name + "' is null." );
        }

        public Opt<T> opt() {
            return Opt.of( doGet() );
        }
    }


    /**
     *
     */
    public static abstract class ReadWrite<C,T>
            extends ReadOnly<C,T> {

        protected ReadWrite( C component, String name ) {
            super( component, name );
        }

        protected abstract void doSet( T newValue );

        /** Set value without firing {@link PropertyChangedEvent}. */
        public ReadWrite<C,T> rawSet( T newValue ) {
            doSet( newValue );
            return this;
        }

        public ReadWrite<C,T> set( T newValue ) {
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
    public static class FieldBackedProperty<C,T>
            extends ReadWrite<C,T> {

        protected T value;

        protected FieldBackedProperty( C component, String name, T initialValue ) {
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
    public static abstract class ReadOnlys<C,T>
            extends Property<C,T> {

        protected ReadOnlys( C component, String name ) {
            super( component, name );
        }

        public abstract Sequence<T,RuntimeException> sequence();
    }


    /**
     *
     */
    public static abstract class ReadWrites<C,T>
            extends ReadOnlys<C,T> {

        protected ReadWrites( C component, String name ) {
            super( component, name );
        }

        protected abstract void doAdd( T value );

        protected abstract void doRemove( T value );

        public <R extends T> R add( R value ) {
            doAdd( value );
            return value;
        }

        public void remove( T value ) {
            doRemove( value );
        }
    }


    /**
     *
     */
    public static class PropertyChangedEvent extends EventObject {

        private Object oldValue;

        private Object newValue;

        protected PropertyChangedEvent( Property<?,?> source, Object oldValue, Object newValue ) {
            super( source );
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        @Override
        public String toString() {
            return "PropertyChangedEvent[source=" + getSource() + ", newValue=" + newValue + "]";
        }

        @Override
        public Property<?,?> getSource() {
            return (Property<?,?>)super.getSource();
        }

        @SuppressWarnings("unchecked")
        public <R> R getOldValue() {
            return (R)oldValue;
        }

        @SuppressWarnings("unchecked")
        public <R> Opt<R> optOldValue() {
            return Opt.of( (R)oldValue );
        }

        @SuppressWarnings("unchecked")
        public <R> R getNewValue() {
            return (R)newValue;
        }

        @SuppressWarnings("unchecked")
        public <R> Opt<R> optNewValue() {
            return Opt.of( (R)newValue );
        }
    }

}
