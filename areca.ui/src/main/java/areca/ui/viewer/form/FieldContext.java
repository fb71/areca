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
import areca.ui.viewer.ViewerBuilder;
import areca.ui.viewer.ViewerContext;
import areca.ui.viewer.model.ModelBase;

/**
 * Extends {@link ViewerContext} by a {@link #label(String)}.
 *
 * @author Falko Bräutigam
 */
public class FieldContext<M extends ModelBase>
        extends ViewerContext<M>
        implements FieldBuilder<M>, FormField {

    private static final Log LOG = LogFactory.getLog( FieldContext.class );

    private String label;

    private String description;


    Viewer<?> _viewer() {
        return viewer;
    }


    @Override
    @SuppressWarnings( "hiding" )
    public FieldContext<M> label( String label ) {
        this.label = label;
        return this;
    }


    @Override
    @SuppressWarnings( "hiding" )
    public ViewerBuilder<M> description( String description ) {
        this.description = description;
        return this;
    }


    @Override
    public UIComponent create() {
        var result = super.create();
        if (label != null) {
            result.addDecorator( new Label().content.set( label ) );
        }
        if (description != null) {
            result.tooltip.set( description );
        }
        return result;
    }


    public UIComponent create( UIComposite parent ) {
        Assert.isNull( label, "Label is not supported yet." );
        return parent.add( create() );
    }


    // FormField ******************************************

    @Override
    @SuppressWarnings( "unchecked" )
    public <R> R currentValue() {
        return (R)currentValue;
    }

}
