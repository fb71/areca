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

/**
 *
 * @param <M> Type of the model value.
 * @param <U> Type of the user interface value.
 * @author Falko Bräutigam
 */
public interface ModelValueTransformer<M,U> {

    public U transform2UI( M value );

    public default M transfor2Model( U value ) {
        throw new RuntimeException( "Implement this method if the field is modifiable." );
    }

}
