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
package areca.ui.viewer.form;

import areca.common.Assert;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.Label;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.viewer.Viewer;
import areca.ui.viewer.ViewerContext;
import areca.ui.viewer.model.ModelBase;

/**
 * Extends {@link ViewerContext} by a {@link #label(String)}.
 *
 * @author Falko Bräutigam
 */
public class FieldContext<M extends ModelBase>
        extends ViewerContext<M>
        implements FieldBuilder<M> {

    private static final Log LOG = LogFactory.getLog( FieldContext.class );

    private String label;


    Viewer<?> _viewer() {
        return viewer;
    }


    public FieldContext<M> label( @SuppressWarnings("hiding") String label ) {
        this.label = label;
        return this;
    }


    @Override
    public UIComponent create() {
        var result = super.create();
        if (label != null) {
            new Label( result ).content.set( label );
        }
        return result;
    }


    public UIComponent create( UIComposite parent ) {
        Assert.isNull( label, "Label is not supported yet." );
        return parent.add( create() );
    }

}
