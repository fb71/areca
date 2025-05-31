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

import static java.lang.Math.round;

/**
 *
 * @author falko
 */
public class Position {

    public static final Position of( int x, int y ) {
        return new Position( x, y );
    }

    // instance *******************************************

    public final int    x;

    public final int    y;

    protected Position( int x, int y ) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return "Position[x=" + x + ", y=" + y + "]";
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + x;
        result = 31 * result + y;
        return result;
    }

    @Override
    public boolean equals( Object other ) {
        return other instanceof Position
                ? x == ((Position)other).x && y == ((Position)other).y
                : false;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public Position substract( Position rhs ) {
        return new Position( x - rhs.x, y - rhs.y );
    }

    public Position substract( int rhs_x, int rhs_y ) {
        return new Position( x - rhs_x, y - rhs_y );
    }

    public Position substract( Size rhs ) {
        return new Position( x - rhs.width(), y - rhs.height() );
    }

    public Position add( Position rhs ) {
        return new Position( x + rhs.x, y + rhs.y );
    }

    public Position add( int rhs_x, int rhs_y ) {
        return new Position( x + rhs_x, y + rhs_y );
    }

    public Position multiply( float factor ) {
        return new Position( round(x * factor), round(y * factor) );
    }

}
