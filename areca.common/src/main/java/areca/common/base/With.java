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
public class With<T> {

    public static <R> With<R> with( R obj ) {
        return new With<>( obj );
    }

    public static <R> With<R> $( R obj ) {
        return with( obj );
    }

    public static <R> With<R> value( R obj ) {
        return with( obj );
    }


    // instance *******************************************

    protected T             obj;

    public With( T obj ) {
        this.obj = obj;
    }

    public <R,E extends Exception> R apply( Function<T,R,E> block ) throws E {
        return block.apply( obj );
    }

    public <E extends Exception> With<T> apply( Consumer<T,E> block ) throws E {
        block.accept( obj );
        return this;
    }

}
