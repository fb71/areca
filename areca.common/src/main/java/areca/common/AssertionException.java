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

import java.util.Optional;

/**
 *
 * @author Falko Bräutigam
 */
public class AssertionException
        extends RuntimeException {

    private Optional<Object> expected = Optional.empty();

    private Optional<Object> actual = Optional.empty();


    public AssertionException( Object expected, Object actual, String msg ) {
        super( msg );
        this.expected = Optional.ofNullable( expected );
        this.actual = Optional.ofNullable( actual );
    }


    public AssertionException( String msg ) {
        super( msg );
    }


    @Override
    public String getMessage() {
        return super.getMessage() + (expected.isPresent() ? " (expected=" + expected.get() + ", actual=" + actual.get() : "");
    }


}
