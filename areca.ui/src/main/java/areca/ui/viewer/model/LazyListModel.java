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

import areca.common.Promise;
import areca.common.base.Consumer.RConsumer;
import areca.common.base.Opt;

/**
 *
 * @author Falko Bräutigam
 */
public interface LazyListModel<V>
        extends ListModelBase<V> {

    public abstract Promise<Integer> count();

    /**
     * Retrieves the elements with the given indexes.
     *
     * @param first The first index to load.
     * @param max The maximum number of values to load.
     * @return {@link Promise} of single values. {@link Opt#absent()} signals last element.
     * @see #load(int, int, RConsumer)
     */
    public abstract Promise<Opt<V>> load( int first, int max );

    public default void load( int first, int max, RConsumer<V> handler ) {
        load( first, max ).onSuccess( opt -> opt.ifPresent( v -> handler.accept( v ) ) );
    }

}
