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
package areca.ui.viewer;

import areca.ui.component2.UIComponent;
import areca.ui.viewer.model.ModelBase;

/**
 *
 * @author Falko Bräutigam
 */
//@FunctionalInterface
//public interface CellBuilder<V,M extends ListModelBase<V>> {
//
//    public UIComponent buildCell( int index, V value, M model, Viewer<M> viewer );
//
//}

@FunctionalInterface
public interface CellBuilder<V> {

    public UIComponent buildCell( int index, V value, ModelBase model, Viewer<?> viewer );

}
