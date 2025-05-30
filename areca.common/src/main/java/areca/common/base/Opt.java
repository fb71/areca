/*
 * Copyright (C) 2020, the @authors. All rights reserved.
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
package areca.common.base;

import java.util.NoSuchElementException;
import java.util.Optional;

import areca.common.Assert;
import areca.common.base.Supplier.RSupplier;

/**
 * Similar to {@link Optional} but allows checked Exceptions.
 *
 * @author Falko Bräutigam
 */
public class Opt<T> {

    @SuppressWarnings("rawtypes")
    private static Opt ABSENT = null; // XXX initializing here does not work with TeaVM

    public static <R> Opt<R> of( R value ) {
        return value != null ? new Opt<>( value ) : absent();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <R> Opt<R> absent() {
        return ABSENT != null ? ABSENT : (ABSENT = new Opt( null ));
    }


    // instance *******************************************

    private T       value;

    protected Opt( T value ) {
        this.value = value;
    }


    @Override
    public String toString() {
        return String.format( "Opt[%s]", isPresent() ? value : "absent" );
    }


    public boolean isPresent() {
        return value != null;
    }

    public boolean isAbsent() {
        return value == null;
    }


    /**
     * If a value is present, performs the given action with the value, otherwise
     * does nothing.
     *
     * @param action The action to be performed, if a value is present.
     * @return this
     * @throws NullPointerException if value is present and the given action is {@code null}.
     * @throws E If the action was throwing this exception.
     */
    public <E extends Exception> Opt<T> ifPresent( Consumer<? super T,E> action ) throws E {
        Assert.notNull( action );
        if (isPresent()) {
            action.accept( value );
        }
        return this;
    }


    /**
     * If a value is not present, performs the given action, otherwise
     * does nothing.
     *
     * @param action The action to be performed, if a value is absent.
     * @return this
     * @throws NullPointerException If value is present and the given action is {@code null}.
     * @throws E If the action was throwing this exception.
     */
    public <E extends Exception> Opt<T> ifAbsent( Runnable action ) throws E {
        Assert.notNull( action );
        if (!isPresent()) {
            action.run();
        }
        return this;
    }


    /**
     * If a value is present and the value matches the given condition then
     * returns this, otherwise return {@link #absent()}.
     */
    public <E extends Exception> Opt<T> matches( Predicate<? super T,E> condition ) throws E {
        Assert.notNull( condition );
        return isPresent() && condition.test( value ) ? this : absent();
    }


    /**
     * If the value {@link #isPresent()} then transform the value with the given
     * mapper function, otherwise return {@link #absent()}.
     *
     * @return Newly created {@link Opt} containing the transformed value, or {@link #absent()}.
     */
    public <R,E extends Exception> Opt<R> transform( Function<? super T,? extends R,E> mapper ) throws E {
        return isPresent() ? Opt.of( mapper.apply( value ) ) : absent();
    }

    /**
     * Synonym for {@link #transform(Function)}.
     */
    public <R,E extends Exception> Opt<R> ifPresentMap( Function<? super T,? extends R,E> mapper ) throws E {
        return transform( mapper );
    }

    /**
     * Synonym for {@link #transform(Function)}.
     */
    public <R,E extends Exception> Opt<R> map( Function<? super T,? extends R,E> mapper ) throws E {
        return transform( mapper );
    }

    /**
     *
     * @throws NoSuchElementException If value is not presnt.
     * @return The value.
     */
    public T get() {
        if (!isPresent()) {
            throw new NoSuchElementException( "The value of this Opt is null.");
        }
        return value;
    }

    public T orNull() {
        return isPresent() ? value : null;
    }

    public T orElse( T elseValue ) {
        return isPresent() ? value : elseValue;
    }

    public <E extends Exception> T orElse( Supplier<T,E> elseSupplier ) throws E {
        return isPresent() ? value : elseSupplier.supply();
    }


    /**
     * If a value is present, returns the value, otherwise throws {@link NoSuchElementException}.
     *
     * @return The non-{@code null} value.
     * @throws NoSuchElementException If no value is present.
     */
    public T orElseError() {
        return orElseError( "No value present" );
    }


    /**
     * If a value is present, returns the value, otherwise throws {@link NoSuchElementException}.
     *
     * @param msg The {@link String#format(String, Object...)} message.
     * @return The non-{@code null} value.
     * @throws NoSuchElementException If no value is present.
     */
    public T orElseError( String msg, Object... args) {
        if (value == null) {
            throw new NoSuchElementException( String.format( msg, args ) );
        }
        return value;
    }


    /**
     * If a value is present, returns the value, otherwise throws an exception
     * produced by the exception supplying function.
     *
     * @throws NullPointerException if no value is present and the exception
     *         supplying function is {@code null}
     */
    public <E extends Throwable> T orElseThrow( RSupplier<E> supplier ) throws E {
        if (value != null) {
            return value;
        } else {
            throw supplier.get();
        }
    }


//    public <E extends Exception> Opt<T> or( Supplier<? extends T,E> supplier ) throws E {
//        return isAbsent() ? Opt.of( Assert.notNull( supplier.supply() ) ) : this;
//    }

    @SuppressWarnings("unchecked")
    public <E extends Exception> Opt<T> or( Supplier<Opt<? extends T>,E> supplier ) throws E {
        return isAbsent() ? Assert.notNull( (Opt<T>)supplier.supply() ) : this;
    }

}
