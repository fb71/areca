/*
 * Copyright (C) 2021, the @authors. All rights reserved.
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
 *
 * @author Falko Bräutigam
 */
public class Scope {

    public static <T,R,E extends Exception> R with( T obj, Function<T,R,E> block ) throws E {
        return block.apply( obj );
    }

    public static <T,E extends Exception> void with( T obj, Consumer<T,E> block ) throws E {
        block.accept( obj );
    }

}
