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

/**
 *
 * @author Falko Bräutigam
 */
public class AssertionException
        extends RuntimeException {

    private Object expected;

    private Object actual;


    public AssertionException( Object expected, Object actual, String msg ) {
        super( msg );
        this.expected = expected;
        this.actual = actual;
    }


    public AssertionException( String msg ) {
        super( msg );
    }


    @Override
    public String getMessage() {
        return super.getMessage() +
                (expected != null && actual != null ? " (expected=" + expected + ", actual=" + actual : "");
    }

}
