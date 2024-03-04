/*
 * Copyright (C) 2019, the @authors. All rights reserved.
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
package areca.ui;

import java.util.logging.Logger;

/**
 *
 * @author falko
 */
public class Color {

    private static final Logger LOG = Logger.getLogger( Color.class.getSimpleName() );

    public static final Color       WHITE = rgb( 0xff, 0xff, 0xff );

    public static Color rgb( int r, int g, int b ) {
        return new Color( r, g, b, (short)0 );
    }

    /**
     * #aabbcc
     */
    public static Color ofHex( String hex ) {
        return new Color(
                Integer.valueOf( hex.substring( 1, 3 ), 16 ),
                Integer.valueOf( hex.substring( 3, 5 ), 16 ),
                Integer.valueOf( hex.substring( 5, 7 ), 16 ),
                0 );
    }

    // instance *******************************************

    public short r, g, b, a;

    protected Color( int r, int g, int b, int a ) {
        this.r = checkValue( r );
        this.g = checkValue( g );
        this.b = checkValue( b );
        this.a = checkValue( a );
    }

    protected short checkValue( int v ) {
        assert v >= 0 && v <= 0xff;
        return (short)v;
    }

    @Override
    public String toString() {
        return toHex();
    }

    public String toHex() {
        return String.format( "#%X%X%X", r, g, b );
    }

}
