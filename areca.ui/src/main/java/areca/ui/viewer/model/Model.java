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

import areca.common.base.Opt;

/**
 *
 * @author Falko Bräutigam
 */
public interface Model<V>
        extends ModelBase {

    /**
     * Reads the value from the underlying data store.
     */
    public abstract V get();

    public abstract void set( V value );


    public default V $() {
        return get();
    }

    public default Opt<V> opt() {
        return Opt.of( get() );
    }

}
