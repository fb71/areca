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

import areca.common.Assert;

/**
 * Similar to {@link java.util.function.Consumer} but allows checked Exceptions.
 *
 * @param <T> the type of the input to the operation
 * @author Falko Bräutigam
 */
@FunctionalInterface
public interface Consumer<T,E extends Exception> {

    /**
     * A {@link Consumer} that does not throw checked Exceptions.
     */
    public interface $<T>
            extends Consumer<T,RuntimeException> {
    }

    /**
     * A {@link Consumer} that does not throw checked Exceptions.
     */
    public interface RConsumer<T>
            extends Consumer<T,RuntimeException> {

        @Override
        default <RE extends RuntimeException> RConsumer<T> andThen( Consumer<? super T,RE> after ) throws RE {
            Assert.notNull( after );
            return (T t) -> {
                accept( t );
                after.accept( t );
            };
        }
    }

    /**
     * Performs this operation on the given argument.
     */
    public void accept( T t ) throws E;


    public default <RE extends E> Consumer<T,E> andThen( Consumer<? super T,RE> after) throws RE {
        Assert.notNull( after );
        return (T t) -> {
            accept( t );
            after.accept( t );
        };
    }

}
