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
package areca.ui.viewer;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;


/**
 *
 * @author Falko Bräutigam
 */
public class Number2StringTransformer
        implements ModelValueTransformer<Number,String> {

    private static final Log LOG = LogFactory.getLog( Number2StringTransformer.class );

    @Override
    public String transform2UI( Number value ) {
        return value != null ? value.toString() : null;
    }

    @Override
    public Number transform2Model( String value ) {
        return value != null ? Integer.valueOf( value ) : null; // XXX other types!?
    }

}
