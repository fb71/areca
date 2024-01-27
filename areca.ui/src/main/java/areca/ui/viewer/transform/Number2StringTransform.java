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
package areca.ui.viewer.transform;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.viewer.model.Model;

/**
 *
 * @author Falko Bräutigam
 */
public class Number2StringTransform
        extends TransformingModelBase<Model<Integer>, String>
        implements Model<String> {

    private static final Log LOG = LogFactory.getLog( Number2StringTransform.class );

    public Number2StringTransform( Model<Integer> delegate ) {
        super( delegate );
    }


    @Override
    public ValidationResult validate( String value ) {
        try {
            Integer.valueOf( value );
            return super.validate( value );
        }
        catch (NumberFormatException e) {
            return new ValidationResult( e );
        }
    }


    @Override
    public String get() {
        var value = delegate.get();
        return value != null ? value.toString() : null;
    }

    @Override
    public void set( String value ) {
        delegate.set( value != null ? Integer.valueOf( value ) : null ); // XXX other types
    }

}
