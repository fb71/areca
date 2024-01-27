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

import org.polymap.model2.Property;

import areca.common.base.Lazy.RLazy;
import areca.common.base.Supplier.RSupplier;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.viewer.model.Model;
import areca.ui.viewer.model.ModelBaseImpl;

/**
 *
 * @author Falko Bräutigam
 */
public class PropertyAdapter<T>
        extends ModelBaseImpl
        implements Model<T> {

    private static final Log LOG = LogFactory.getLog( PropertyAdapter.class );

    protected RLazy<Property<T>>        prop;

    protected PropertyAdapter( RSupplier<Property<T>> supplier ) {
        this.prop = new RLazy<>( supplier );
    }

    @Override
    public T get() {
        return prop.supply().get();
    }

    @Override
    public void set( T value ) {
        prop.supply().set( value );
    }

}
