/*
 * polymap.org
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
public class Point {

    private int         x;

    private int         y;

    public Point( int x, int y ) {
        this.x = x;
        this.y = y;
    }


    public int x() {
        return x;
    }


    public int y() {
        return y;
    }

}
