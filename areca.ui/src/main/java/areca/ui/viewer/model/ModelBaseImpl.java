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

import areca.common.event.EventManager;

/**
 *
 * @author Falko Bräutigam
 */
public abstract class ModelBaseImpl
        implements ModelBase {

//    private boolean disposed;
//
//    public void dispose() {
//        this.disposed = true;
//    }
//
//    public boolean isDisposed() {
//        return disposed;
//    }


    public void fireChangeEvent() {
        EventManager.instance().publish( new ModelChangeEvent( this ) );
    }

}
