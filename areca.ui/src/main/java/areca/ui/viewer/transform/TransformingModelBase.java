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
package areca.ui.viewer.transform;

import areca.ui.viewer.model.ModelBase;

/**
 * The base of all transforming model adapters.
 *
 * @param <M> The type of the delegate model.
 * @param <V> The type we are transforming into.
 * @author Falko Bräutigam
 */
public abstract class TransformingModelBase<M extends ModelBase, V>
        implements ModelBase, ValidatingModel<V> {

    protected M delegate;

    public TransformingModelBase( M delegate ) {
        this.delegate = delegate;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ValidationResult validate( V value ) {
        return delegate instanceof ValidatingModel
                ? ((ValidatingModel<V>)delegate).validate( value )
                : VALID;
    }

}
