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
package areca.common.base;

import areca.common.Assert;

/**
 * Similar to {@link java.util.function.BiConsumer} but allows checked Exceptions.
 *
 * @param <T1> The type of the first input element
 * @param <T2> The type of the second input element
 * @param <T3> The type of the third input element
 * @author Falko Bräutigam
 */
@FunctionalInterface
public interface TriConsumer<T1,T2,T3,E extends Exception> {

    public interface RBiConsumer<T1,T2,T3>
            extends TriConsumer<T1,T2,T3,RuntimeException> {
    }

    /**
     * Performs this operation on the given argument.
     */
    public void accept( T1 v1, T2 v2, T3 v3 ) throws E;


    public default <RE extends E> TriConsumer<T1,T2,T3,E> andThen( TriConsumer<? super T1,? super T2,? super T3,RE> after) throws RE {
        Assert.notNull( after );
        return (T1 v1, T2 v2, T3 v3) -> {
            accept(v1, v2, v3);
            after.accept(v1, v2, v3);
        };
    }

}
