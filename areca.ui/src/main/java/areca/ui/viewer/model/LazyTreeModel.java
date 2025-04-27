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

import java.util.List;

import areca.common.Promise;

/**
 *
 * @author Falko Bräutigam
 */
public interface LazyTreeModel<V>
        extends TreeModelBase<V> {

    //public Promise<Integer> countChildren( V item );

    public Promise<List<? extends V>> loadChildren( V item, int first, int max );

    public default Promise<List<? extends V>> loadAllChildren( V item ) {
        return loadChildren( item, 0, Integer.MAX_VALUE );
    }


}
