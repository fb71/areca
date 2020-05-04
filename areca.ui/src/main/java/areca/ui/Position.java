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

/**
 *
 * @author falko
 */
public class Position {

    public static final Position of( int x, int y ) {
        return new Position( x, y );
    }

    // instance *******************************************

    private int         x;

    private int         y;

    protected Position( int x, int y ) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return "Position[x=" + x + ", y=" + y + "]";
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

}
