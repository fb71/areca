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
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import areca.common.Assert;

/**
 * Similar to {@link Optional} but allows checked Exceptions.
 *
 * @author Falko Bräutigam
 */
public class Opt<T> {

    /** Common instance for {@code empty()}.  */
    @SuppressWarnings("rawtypes")
    private static final Opt EMPTY = new Opt<>( null );

    public static <R> Opt<R> ofNullable( R value ) {
        return value != null ? new Opt<>( value ) : EMPTY;
    }

    public static <R> Opt<R> of( R value ) {
        return new Opt<>( Objects.requireNonNull( value ) );
    }

    public static <R> Opt<R> absent() {
        return EMPTY;
    }


    // instance *******************************************

    private T       value;

    protected Opt( T value ) {
        this.value = value;
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
     * @throws NullPointerException if value is present and the given action is {@code null}.
     * @throws E If the action was throwing this exception.
     */
    public <E extends Exception> void ifPresent( Consumer<? super T,E> action ) throws E {
        Assert.notNull( action );
        if (isPresent()) {
            action.accept( value );
        }
    }


    /**
     * If a value is not present, performs the given action with the value, otherwise
     * does nothing.
     *
     * @param action The action to be performed, if a value is absent.
     * @throws NullPointerException If value is present and the given action is {@code null}.
     * @throws E If the action was throwing this exception.
     */
    public <E extends Exception> void ifAbsent( Consumer<? super T,E> action ) throws E {
        Assert.notNull( action );
        if (!isPresent()) {
            action.accept( value );
        }
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
     * @return The transformed value, or {@link #absent()}.
     */
    public <R,E extends Exception> Opt<R> transform( Function<? super T,? extends R,E> mapper ) throws E {
        return isPresent() ? Opt.ofNullable( mapper.apply( value ) ) : absent();
    }


    public <R,E extends Exception> Opt<R> ifPresentTransform( Function<? super T,? extends R,E> mapper ) throws E {
        return isPresent() ? Opt.ofNullable( mapper.apply( value ) ) : absent();
    }


    /**
     * Synonym for {@link #transform(Function)}.
     */
    public <R,E extends Exception> Opt<R> ifPresentMap( Function<? super T,? extends R,E> mapper ) throws E {
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


    public T orElse( T elseValue ) {
        return isPresent() ? value : elseValue;
    }


    /**
     * If a value is present, returns the value, otherwise throws {@link NoSuchElementException}.
     *
     * @return The non-{@code null} value.
     * @throws NoSuchElementException If no value is present.
     */
    public T orElseThrow() {
        if (value == null) {
            throw new NoSuchElementException( "No value present" );
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
    public <E extends Throwable> T orElseThrow( Supplier<E> supplier ) throws E {
        if (value != null) {
            return value;
        } else {
            throw supplier.get();
        }
    }

}
