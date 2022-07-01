/*
 * Copyright (C) 2022, the @authors. All rights reserved.
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
public class Text
        extends UIComponent {

    public enum Format {
        PLAIN, PREFORMATTED, HTML
    }

    public ReadWrite<Text,String> content = Property.rw( this, "content" );

    /**
     * The format of the content, defaults to {@link Format#PLAIN}.
     */
    public ReadWrite<Text,Format> format = Property.rw( this, "format", Format.PLAIN );

}
