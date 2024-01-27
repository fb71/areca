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
package areca.ui.viewer.transform;

import areca.ui.viewer.model.ModelBase;
import areca.ui.viewer.model.ModelBase.ValidationResult;

/**
 *
 * @param <V>
 * @author Falko Bräutigam
 */
public interface ValidatingModel<V> {

    /**
     * Checks if the given value would cause an error if send to the backend via
     * {@link #set(Object)}.
     *
     * @return {@link ModelBase#VALID} or other result that describes the validation
     *         error.
     */
    ValidationResult validate( V value );

}