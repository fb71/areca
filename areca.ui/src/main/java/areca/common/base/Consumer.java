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
 * Similar to {@link java.util.function.Consumer} but allows checked Exceptions.
 *
 * @param <T> the type of the input to the operation
 * @author Falko Bräutigam
 */
@FunctionalInterface
public interface Consumer<T,E extends Exception> {

    /**
     * Performs this operation on the given argument.
     */
    void accept( T t ) throws E;

}
