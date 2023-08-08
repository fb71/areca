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

    public static final boolean enabled = true;


    protected static String message( String[] msgs, String defaultMsg ) {
        return msgs.length > 0 ? String.join( "", msgs ) : defaultMsg;
    }


    public static void that( boolean cond, String... msgs ) throws AssertionException {
        if (enabled && !cond) {
            throw new AssertionException( message( msgs, "condition not met" ) );
        }
    }


    public static void that( Supplier<Boolean> cond, String... msgs ) {
        if (enabled && !cond.get()) {
            throw new AssertionException( message( msgs, "condition not met" ) );
        }
    }


    public static void that( Supplier<Boolean> cond, Supplier<String> msg ) {
        if (enabled && !cond.get()) {
            throw new AssertionException( msg.get() );
        }
    }


    public static void isEqual( Object expected, Object actual, String... msgs ) throws AssertionException {
        if (enabled && !Objects.equals( actual, expected )) {
            throw new AssertionException( expected, actual, message( msgs, "not equal" ) );
        }
    }


    public static void isSame( Object expected, Object actual, String... msgs ) throws AssertionException {
        if (enabled && actual != expected) {
            throw new AssertionException( expected, actual, message( msgs, "not same" ) );
        }
    }


    public static <R> R notSame( Object expected, R actual, String... msgs ) throws AssertionException {
        if (enabled && actual == expected) {
            throw new AssertionException( expected, actual, message( msgs, "is same" ) );
        }
        return actual;
    }

    @SuppressWarnings("unchecked")
    public static <R> R isType( Class<?> expected, Object actual, String... msgs ) throws AssertionException {
        if (enabled && actual != null && !expected.isAssignableFrom( actual.getClass() )) {
            throw new AssertionException( expected, actual, message( msgs, "not expected type" ) );
        }
        return (R)actual;
    }


    public static <R> R isNull( R actual, String... msgs ) {
        if (enabled && actual != null) {
            throw new AssertionException( null, actual, message( msgs, "null expected" ) );
        }
        return actual;
    }


    public static <R> R notNull( R actual, String... msgs ) {
        if (enabled && actual == null) {
            throw new AssertionException( null, actual, message( msgs, "non-null expected" ) );
        }
        return actual;
    }
}
