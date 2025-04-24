/*
 * Copyright (C) 2025, the @authors. All rights reserved.
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
public interface LazyTreeModel<V>
        extends TreeModelBase<V> {

    public abstract Promise<Integer> countBranches();

    /**
     * Retrieves the branch elements with the given indexes.
     *
     * @param first The first index to load.
     * @param max The maximum number of values to load.
     * @return {@link Promise} of single values. {@link Opt#absent()} signals last element.
     * @see #load(int, int, RConsumer)
     */
    public abstract Promise<Opt<LazyTreeModel<V>>> loadBranches( int first, int max );

    public abstract Promise<Integer> countLeafs();

    /**
     * Retrieves the branch elements with the given indexes.
     *
     * @param first The first index to load.
     * @param max The maximum number of values to load.
     * @return {@link Promise} of single values. {@link Opt#absent()} signals last element.
     * @see #load(int, int, RConsumer)
     */
    public abstract Promise<Opt<V>> loadLeafs( int first, int max );

}
