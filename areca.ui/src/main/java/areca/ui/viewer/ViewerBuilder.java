/*
 * Copyright (C) 2024, the @authors. All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software
 * Foundation; either version 3.0 of the License, or (at your option) any later
 * version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 */
package areca.ui.viewer;

import areca.ui.component2.UIComponent;
import areca.ui.viewer.model.ModelBase;

/**
 * Provides a fluent interface to create a {@link Viewer} with transformation and
 * validation.
 *
 * @author Falko Bräutigam
 */
public interface ViewerBuilder<M extends ModelBase> {

    <R extends M> ViewerBuilder<R> viewer( Viewer<R> viewer );

    ViewerBuilder<M> model( M adapter );

    /**
     * Initializes this viewer and creates the {@link UIComponent} but does not load
     * the value from the model.
     *
     * @return Newly created {@link UIComponent}
     */
    UIComponent create();

    /**
     * Initializes this viewer and creates the {@link UIComponent} and loads the
     * model value. Intended to be used by dynamic/complex model that create viewer
     * on demand.
     *
     * @return Newly created {@link UIComponent}
     */
    UIComponent createAndLoad();

}