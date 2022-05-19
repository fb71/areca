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
package areca.ui.form;

import areca.common.Assert;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.TextField;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.viewer.ViewerBuilder;

/**
 *
 * @author Falko Bräutigam
 */
public class FieldBuilder
        extends ViewerBuilder {

    private static final Log LOG = LogFactory.getLog( FieldBuilder.class );

    private String                      label;


    public FieldBuilder label( @SuppressWarnings("hiding") String label ) {
        this.label = label;
        return this;
    }


    @Override
    public UIComponent create() {
        var result = super.create();
        if (label != null && result instanceof TextField) {
            ((TextField)result).label.set( label );
        }
        return result;
    }


    public UIComponent create( UIComposite parent ) {
        Assert.isNull( label, "Label is not supported yet." );
        return parent.add( create() );
    }

}
