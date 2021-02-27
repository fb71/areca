/*
 * Copyright (C) 2020, the @authors. All rights reserved.
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
package areca.ui.controller;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component.Property;

/**
 *
 * @author Falko Bräutigam
 */
public class State<T>
        extends Property<T> {

    private static final Log log = LogFactory.getLog( State.class );

    protected State( Object component, String name, T value ) {
        super( component, name, value );
    }

}
