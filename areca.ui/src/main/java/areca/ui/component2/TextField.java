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

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.Property.ReadWrite;

/**
 *
 * @author Falko Bräutigam
 */
public class TextField
        extends UIComponent {

    private static final Log LOG = LogFactory.getLog( TextField.class );

    public ReadWrite<TextField,String> content = Property.rw( this, "content" );

    public ReadWrite<TextField,Boolean> multiline = Property.rw( this, "multiline", false );

    //public ReadWrite<TextField,String> label = Property.rw( this, "label" );

    @Override
    public int computeMinHeight( int width ) {
        return 32;
    }


}
