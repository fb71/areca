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

import areca.common.Assert;
import areca.common.base.Function;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.layout.FillLayout;
import areca.ui.layout.RowConstraints;

/**
 *
 * @author Falko Bräutigam
 */
@SuppressWarnings("rawtypes")
public class ViewerBuilder {

    private static final Log LOG = LogFactory.getLog( ViewerBuilder.class );

    public interface TransformerMapping<M>
            extends Function<SingleValueAdapter<M>, ModelValueTransformer<M,?>, RuntimeException> {
    }

    // instance *******************************************

    protected Viewer                    viewer;

    protected ModelAdapter              adapter;

    protected ModelValueTransformer     transformer;

    protected ModelValueValidator<?>    validator;


    @SuppressWarnings("hiding")
    public ViewerBuilder viewer( Viewer<?> viewer ) {
        this.viewer = viewer;
        return this;
    }

    @SuppressWarnings("hiding")
    public ViewerBuilder adapter( ModelAdapter adapter ) {
        this.adapter = adapter;
        return this;
    }

    @SuppressWarnings("hiding")
    public ViewerBuilder transformer( ModelValueTransformer<?,?> transformer ) {
        this.transformer = transformer;
        return this;
    }

    @SuppressWarnings("hiding")
    public ViewerBuilder validator( ModelValueValidator<?> validator ) {
        this.validator = validator;
        return this;
    }


    public UIComponent create() {
        Assert.notNull( adapter, "Adapter is mandatory for building a viewer!" );
        if (adapter instanceof SingleValueAdapter) {
            viewer.init( new TransformingSingleValueAdapter() );
        }
        else {
            throw new RuntimeException( "Unknown adapter type: " + adapter );
        }
        viewer = viewer != null ? viewer : guessViewer();
        var field = viewer.create();

        // container for label and valid decorators
        return new UIComposite() {{
            // FIXME hack! UIComposite does not calculate its minHeight depending on its children (yet:)
            layoutConstraints.set( new RowConstraints().height.set( field.computeMinHeight( 400 ) ) );
            layout.set( new FillLayout() );
            add( field );
        }};
    }


    protected Viewer guessViewer() {
        throw new RuntimeException( "not yet implemented." );
    }


    /**
     *
     */
    protected class TransformingSingleValueAdapter
            implements SingleValueAdapter {

        @Override
        public Object getValue() {
            Object value = ((SingleValueAdapter)adapter).getValue();
            if (transformer != null) {
                value = transformer.transform2UI( value );
            }
            if (validator != null) {
                throw new RuntimeException( "not yet implemented: validator" );
            }
            return value;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setValue( Object value ) {
            if (transformer != null) {
                value = transformer.transform2Model( value );
            }
            if (validator != null) {
                throw new RuntimeException( "not yet implemented: validator" );
            }
            ((SingleValueAdapter)adapter).setValue( value );
        }
    }

}
