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
 * Similar to {@link java.util.function.Function} but allows checked Exceptions.
 *
 * @author Falko Bräutigam
 */
@FunctionalInterface
public interface Function<T,R,E extends Exception> {

    /**
     * A {@link Function} that does not throw checked Exceptions.
     */
    public interface RFunction<T,R>
            extends Function<T,R,RuntimeException> {
    }

    public R apply( T t ) throws E;

}
