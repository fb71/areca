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

    /** Common instance for {@code empty()}.  */
    @SuppressWarnings("rawtypes")
    private static final Opt ABSENT = new Opt<>( null );

    public static <R> Opt<R> of( R value ) {
        return value != null ? new Opt<>( value ) : ABSENT;
    }

//    public static <R> Opt<R> of( R value ) {
//        return new Opt<>( Objects.requireNonNull( value ) );
//    }

    public static <R> Opt<R> absent() {
        return ABSENT;
    }


    // instance *******************************************

    private T       value;

    protected Opt( T value ) {
        this.value = value;
    }


    @Override
    public String toString() {
        return String.format( "Opt[%s]", value );
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
     * If a value is not present, performs the given action with the value, otherwise
     * does nothing.
     *
     * @param action The action to be performed, if a value is absent.
     * @return this
     * @throws NullPointerException If value is present and the given action is {@code null}.
     * @throws E If the action was throwing this exception.
     */
    public <E extends Exception> Opt<T> ifAbsent( Consumer<? super T,E> action ) throws E {
        Assert.notNull( action );
        if (!isPresent()) {
            action.accept( value );
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
     * @return The transformed value, or {@link #absent()}.
     */
    public <R,E extends Exception> Opt<R> transform( Function<? super T,? extends R,E> mapper ) throws E {
        return isPresent() ? Opt.of( mapper.apply( value ) ) : absent();
    }


    public <R,E extends Exception> Opt<R> ifPresentTransform( Function<? super T,? extends R,E> mapper ) throws E {
        return isPresent() ? Opt.of( mapper.apply( value ) ) : absent();
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

    public <E extends Exception> T orElseCompute( Supplier<T,E> elseSupplier ) throws E {
        return isPresent() ? value : elseSupplier.supply();
    }


    /**
     * If a value is present, returns the value, otherwise throws {@link NoSuchElementException}.
     *
     * @return The non-{@code null} value.
     * @throws NoSuchElementException If no value is present.
     */
    public T orElseError() {
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
    public <E extends Throwable> T orElseThrow( RSupplier<E> supplier ) throws E {
        if (value != null) {
            return value;
        } else {
            throw supplier.get();
        }
    }

}
