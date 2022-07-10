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
package areca.app.service.matrix;

import areca.app.model.MatrixSettings;
import areca.app.model.StoreRef;

/**
 *
 * @author Falko Bräutigam
 */
abstract class MatrixStoreRef
        extends StoreRef {

    /** Decode */
    public MatrixStoreRef() { }

    public MatrixStoreRef( MatrixSettings settings ) {
        this.parts.add( settings.username.get() );
    }

    @Override
    public String prefix() {
        return "matrix-";
    }

    public String serviceUsername() {
        return parts.get( 0 );
    }
}
