/*
 * Copyright (C) 2019-2022, the @authors. All rights reserved.
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
package areca.ui.component2;

import areca.ui.component2.Property.ReadWrite;

/**
 *
 * @author falko
 */
public class Button
        extends UIComponent {

    public enum Format {
        PLAIN, HTML
    }

    public ReadWrite<Button,String> label = new ReadWrite<>( this, "label" );

    /**
     * The format of the {@link #label}; defaults to {@link Format#PLAIN}.
     */
    public ReadWrite<Button,Format> format = Property.rw( this, "format", Format.PLAIN );

    /**
     * A ligature or numeric character reference of a
     * <a href="https://fonts.google.com/icons">Material Icon</a>.
     */
    public ReadWrite<Button,String> icon = new ReadWrite<>( this, "icon" );

    /**
     * Base64 encoded image.
     */
    public ReadWrite<Button,String> image = new ReadWrite<>( this, "imageData" );

    {
        bordered.set( true );
    }

}
