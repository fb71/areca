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
package areca.ui.viewer.model;

/**
 * A {@link Model} that holds a simple POJO as value.
 *
 * @author Falko Bräutigam
 */
public class Pojo<V>
        extends ModelBaseImpl
        implements Model<V> {

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
