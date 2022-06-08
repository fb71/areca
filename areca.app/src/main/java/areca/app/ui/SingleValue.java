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
package areca.app.ui;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.viewer.SingleValueAdapter;

/**
 *
 * @author Falko Bräutigam
 */
public class SingleValue<T>
        implements SingleValueAdapter<T> {

    private static final Log LOG = LogFactory.getLog( SingleValue.class );

    protected T value;


    public SingleValue( T value ) {
        this.value = value;
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public void setValue( T value ) {
        this.value = value;
    }

}
