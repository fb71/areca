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
 * Similar to {@link java.util.function.BiConsumer} but allows checked Exceptions.
 *
 * @param <T1> The type of the first input element
 * @param <T2> The type of the second input element
 * @author Falko Bräutigam
 */
@FunctionalInterface
public interface BiConsumer<T1,T2,E extends Exception> {

    public interface RBiConsumer<T1,T2>
            extends BiConsumer<T1,T2,RuntimeException> {
    }

    /**
     * Performs this operation on the given argument.
     */
    public void accept( T1 v1, T2 v2 ) throws E;


    public default <RE extends E> BiConsumer<T1,T2,E> andThen( BiConsumer<? super T1,? super T2,RE> after) throws RE {
        Assert.notNull( after );
        return (T1 v1, T2 v2) -> {
            accept(v1, v2);
            after.accept(v1, v2);
        };
    }

}
