/*
 * Copyright (C) 2020, the @authors. All rights reserved.
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
import areca.ui.modeladapter.ModelValueBase;

/**
 *
 * @author Falko Bräutigam
 */
public abstract class Viewer<A extends ModelValueBase> {

    protected A         model;


    @SuppressWarnings("hiding")
    protected Viewer<A> init( A model ) {
        this.model = model;
        return this;
    }


    public abstract UIComponent create();

    public abstract void store();

    public abstract void load();
}
