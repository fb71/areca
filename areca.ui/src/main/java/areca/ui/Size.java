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
public class Size {

    public static final Size of( int width, int height ) {
        return new Size( width, height );
    }

    // instance *******************************************

    private final int       width;

    private final int       height;

    protected Size( int width, int height ) {
        this.width = width;
        this.height = height;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    @Override
    public String toString() {
        return "Size[width=" + width + ", height=" + height + "]";
    }

    public Position toPosition() {
        return Position.of( width, height );
    }

    public Size divide( float divisor  ) {
        return new Size( round(width / divisor), round(height / divisor) );
    }

}
