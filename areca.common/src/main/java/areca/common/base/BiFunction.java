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

/**
 * Similar to {@link java.util.function.BiFunction} but allows checked Exceptions.
 *
 * @author Falko Bräutigam
 */
@FunctionalInterface
public interface BiFunction<T1,T2,R,E extends Exception> {

    /**
     * A {@link BiFunction} that does not throw checked Exceptions.
     */
    public interface RBiFunction<T1,T2,R>
            extends BiFunction<T1,T2,R,RuntimeException> {
    }

    public R apply( T1 t1, T2 t2 ) throws E;

}
