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
package areca.ui.component2;

import java.util.Collection;
import java.util.EventObject;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

import areca.common.Assert;
import areca.common.base.BiConsumer.RBiConsumer;
import areca.common.base.Opt;
import areca.common.base.Sequence;
import areca.common.base.Supplier.RSupplier;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @param <T> The type of the value of this property.
 * @param <C> The type of the component this is a property of.
 * @author falko
 */
public abstract class Property<C,T> {

    private static final Log LOG = LogFactory.getLog( Property.class );

    /** Creates a field backed read-write property. */
    public static <RC,R> ReadWrite<RC,R> rw( RC component, String name ) {
        return new ReadWrite<>( component, name );
    }

    /** Creates a read-write property with the given initial value. */
    public static <RC,R> ReadWrite<RC,R> rw( RC component, String name, R initValue ) {
        return new ReadWrite<>( component, name, initValue );
    }

    /** Creates ... */
    public static <RC,R> ReadWrites<RC,R> rws( RC component, String name ) {
        return new ReadWrites<>( component, name );
    }

    /** Creates ... */
    public static <RC,R> ReadWrites<RC,R> rws( RC component, String name, Collection<R> initValue ) {
        return new ReadWrites<>( component, name, initValue );
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

    public C component() {
        return component;
    }

    public String name() {
        return name;
    }

    protected void fireEvent( T oldValue, T newValue ) {
        if (Objects.equals( oldValue, newValue )) {
            LOG.debug( "FIRE:" + name() + ": values are EQUAL: " + newValue + " -- " + oldValue );
        }
        var ev = new PropertyChangedEvent<>( this, oldValue, newValue );
        UIComponentEvent.manager.publish( ev );
    }


    /**
     *                  Immutable
     */
    public static class ReadOnly<C,T>
            extends Property<C,T> {

        protected RSupplier<T>  valueSupplier;

        protected T             value;

        protected boolean       valuePresent;

        protected ReadOnly( C component, String name ) {
            super( component, name );
        }

        protected ReadOnly( C component, String name, T initValue ) {
            super( component, name );
            this.value = initValue;
            this.valuePresent = true;
        }

        /**
         * Supplies a value that is used as default if the {@link #value()} would be null.
         */
        public ReadOnly<C,T> defaultsTo( @SuppressWarnings("hiding") RSupplier<T> valueSupplier ) {
            Assert.that( !valuePresent, "A value is already present: " + name );
            Assert.isNull( this.valueSupplier, "A value supplier already present: " + name );
            this.valueSupplier = Assert.notNull( valueSupplier );
            return this;
        }

        public T value() {
            return opt().orElseThrow( () -> new NoSuchElementException( name() ) );
        }

        public T get() {
            return value();
        }

        public T $() {
            return value();
        }

        public T __() {
            return value();
        }

        public Opt<T> opt() {
            if (!valuePresent & valueSupplier != null) {
                valuePresent = true; // before supplier! otherwise Component.clientSize cannot reset
                value = valueSupplier.supply();
                LOG.debug( "DEFAULT: %s -> %s", name, value );
            }
            return Opt.of( value );
        }

        @Override
        public String toString() {
            return String.format( "%s = %s", name, opt().orElse( null ) );
        }
    }


    /**
     *
     */
    public static class ReadWrite<C,T>
            extends ReadOnly<C,T> {

        protected ReadWrite( C component, String name ) {
            super( component, name );
        }

        protected ReadWrite( C component, String name, T initValue ) {
            super( component, name, initValue );
        }

        /**
         * For framework/internal use: sets the value without firing event.
         */
        public ReadWrite<C,T> rawSet( T newValue ) {
            value = newValue;
            valuePresent = true;
            return this;
        }

        public C set( T newValue ) {
            var oldValue = value;
            rawSet( newValue );
            fireEvent( oldValue, newValue );
            LOG.debug( "SET: %s:%s -> %s (%s)", component().getClass().getSimpleName(), name, newValue, oldValue );
            return component;
        }

        public ReadWrite<C,T> onChange( RBiConsumer<T,T> consumer ) {
            UIComponentEvent.manager
                    .subscribe( (PropertyChangedEvent<T> ev) -> {
                        //LOG.debug( "HANDLE: %s:%s -> %s (%s)", component().getClass().getSimpleName(), name, ev.getNewValue(), ev.getOldValue() );
                        consumer.accept( ev.getNewValue(), ev.getOldValue() );
                    })
                    .performIf( ev -> ev.getSource() == ReadWrite.this )
                    .unsubscribeIf( () -> ((UIComponent)component()).isDisposed() ); // TODO
            return this;
        }

        public ReadWrite<C,T> onInitAndChange( RBiConsumer<T,T> consumer ) {
            if (valuePresent) {
                consumer.accept( value, null );
            }
            return onChange( consumer );
        }
    }


    /**
     *
     */
    public static class ReadWrites<C,T>
            extends ReadWrite<C,Collection<T>>
            implements Iterable<T> {

        protected ReadWrites( C component, String name ) {
            super( component, name );
        }

        protected ReadWrites( C component, String name, Collection<T> initValue ) {
            super( component, name, initValue );
        }

        public Sequence<T,RuntimeException> values() {
            return Sequence.of( value() );
        }

        @Override
        public Iterator<T> iterator() {
            return value.iterator();
        }

        /**
         * @return The newly added element if it was successfully added,
         *         {@link Opt#absent()} otherwise.
         */
        public Opt<T> add( T add ) {
            var oldValue = values().toList();
            try {
                LOG.debug( "ADD: %s:%s -> %s + %s", component().getClass().getSimpleName(), name, add, oldValue );
                valuePresent = true;
                return value().add( add ) ? Opt.of( add ) : Opt.absent();
            }
            finally {
                fireEvent( oldValue, values().toList() );
            }
        }

        public Opt<T> remove( T remove ) {
            var oldValue = values().toList();
            try {
                LOG.debug( "REMOVE: %s:%s -> %s + %s", component().getClass().getSimpleName(), name, remove, oldValue );
                valuePresent = true;
                return value().remove( remove ) ? Opt.of( remove ) : Opt.absent();
            }
            finally {
                fireEvent( oldValue, values().toList() );
            }
        }

        /**
         *
         * @param elm
         * @param add True signals that the given element is to be added. Remove otherwise.
         * @return See {@link #add(Object)} and {@link #remove(Object)}
         */
        public Opt<T> modify( T elm, boolean add ) {
            return add ? add( elm ) : remove( elm );
        }
    }


//    /**
//     *
//     */
//    public static class FieldBackedProperties<C,T>
//            extends ReadWrites<C,T> {
//
//        protected Collection<T> values;
//
//        protected FieldBackedProperties( C component, String name, Collection<T> initialValues ) {
//            super( component, name );
//            this.values = initialValues;
//        }
//
//        @Override
//        public ReadWrites<C,T> rawSet( Collection<T> newValues ) {
//            values = newValues;
//            initialized = true;
//            return this;
//        }
//
//        @Override
//        protected boolean doAdd( T value ) {
//            return values.add( value );
//        }
//
//        @Override
//        protected boolean doRemove( T value ) {
//            return values.remove( value );
//        }
//
//        @Override
//        public Sequence<T,RuntimeException> sequence() {
//            return Sequence.of( values );
//        }
//    }


    /**
     *
     */
    public static class PropertyChangedEvent<V> extends EventObject {

        private Object oldValue;

        private Object newValue;

        protected PropertyChangedEvent( Property<?,?> source, Object oldValue, Object newValue ) {
            super( source );
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        @Override
        public String toString() {
            return String.format( "PropertyChangedEvent[%s::%s, newValue=%s]",
                    getSource().component().getClass().getSimpleName(),
                    getSource().name(),
                    newValue);
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
