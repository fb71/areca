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
package areca.ui.modeladapter;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * A {@link ModelValue} that holds a simple POJO as value.
 *
 * @author Falko Bräutigam
 */
public class Pojo<V>
        extends ModelValue<V> {

    private static final Log LOG = LogFactory.getLog( Pojo.class );

    private V value;

    public Pojo() {
    }

    public Pojo( V value ) {
        this.value = value;
    }

    @Override
    public V get() {
        return value;
    }

    @Override
    public void set( V value ) {
        this.value = value;
        fireChangeEvent();
    }

}
