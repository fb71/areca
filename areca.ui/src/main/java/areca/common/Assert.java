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
package areca.common;

import java.util.Objects;
import java.util.function.Supplier;

/**
 *
 * @author Falko Bräutigam
 */
public class Assert {

    public static volatile boolean enabled = true;


    public static void that( boolean cond, String msg ) throws AssertionException {
        if (enabled && !cond) {
            throw new AssertionException( msg );
        }
    }


    public static void that( Supplier<Boolean> cond, String msg ) {
        if (enabled && !cond.get()) {
            throw new AssertionException( msg );
        }
    }


    public static void that( Supplier<Boolean> cond, Supplier<String> msg ) {
        if (enabled && !cond.get()) {
            throw new AssertionException( msg.get() );
        }
    }


    public static void equals( Object expected, Object actual, String msg ) throws AssertionException {
        if (enabled && !Objects.equals( actual, expected )) {
            throw new AssertionException( expected, actual, msg );
        }
    }


    public static void same( Object expected, Object actual, String msg ) throws AssertionException {
        if (enabled && actual != expected) {
            throw new AssertionException( expected, actual, msg );
        }
    }


    public static <R> R isNull( R actual ) {
        if (enabled && actual != null) {
            throw new AssertionException( null, actual, "null expected!" );
        }
        return actual;
    }


    public static <R> R notNull( R actual ) {
        if (enabled && actual == null) {
            throw new AssertionException( null, actual, "non-null expected!" );
        }
        return actual;
    }
}
