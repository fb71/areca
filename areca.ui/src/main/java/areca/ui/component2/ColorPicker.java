/*
 * Copyright (C) 2024, the @authors. All rights reserved.
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

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.Property.ReadWrite;

/**
 *
 * @author Falko Bräutigam
 */
public class ColorPicker
        extends UIComponent {

    private static final Log LOG = LogFactory.getLog( ColorPicker.class );

    /** 7-character string specifying an RGB color in hexadecimal format: #001122 */
    public ReadWrite<ColorPicker,String> value = Property.rw( this, "value" );

    @Override
    public int computeMinHeight( int width ) {
        return 32;
    }

}
