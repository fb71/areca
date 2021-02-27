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
import areca.ui.component.UIComponent;
import areca.ui.component.UIComposite;
import areca.ui.viewer.ModelAdapter;
import areca.ui.viewer.ModelValueTransformer;
import areca.ui.viewer.ModelValueValidator;
import areca.ui.viewer.SingleValueAdapter;

/**
 *
 * @author Falko Bräutigam
 */
@SuppressWarnings("rawtypes")
public class ViewerBuilder {

    private static final Log log = LogFactory.getLog( ViewerBuilder.class );

    public interface TransformerMapping<M>
            extends Function<SingleValueAdapter<M>, ModelValueTransformer<M,?>, RuntimeException> {
    }

    // instance *******************************************

    private Viewer                      viewer;

    private ModelAdapter                adapter;

    private ModelValueTransformer<?,?>  transformer;

    //private List<TransformerMapping<?>> transformerMappings = new ArrayList<>();

    private ModelValueValidator<?>      validator;


    public ViewerBuilder viewer( @SuppressWarnings("hiding") Viewer<?> viewer ) {
        this.viewer = viewer;
        return this;
    }


    public ViewerBuilder adapter( @SuppressWarnings("hiding") ModelAdapter adapter ) {
        this.adapter = adapter;
        return this;
    }


    public ViewerBuilder transformer( @SuppressWarnings("hiding") ModelValueTransformer<?,?> transformer ) {
        this.transformer = transformer;
        return this;
    }


//    public <M> ViewerBuilder transformer( TransformerMapping<M> mapping ) {
//        this.transformerMappings.add( mapping );
//        return this;
//    }


    public ViewerBuilder validator( @SuppressWarnings("hiding") ModelValueValidator<?> validator ) {
        this.validator = validator;
        return this;
    }


    public UIComponent create( UIComposite parent ) {
        viewer = viewer != null ? viewer : guessViewer();

        Assert.notNull( adapter, "Adapter is mandatory for building a viewer!" );
        if (adapter instanceof SingleValueAdapter) {
            viewer.init( createSingleValueAdapter( (SingleValueAdapter)adapter ) );
        }
        else {
            throw new RuntimeException( "Unknown adapter type: " + adapter );
        }
        return viewer.create( parent );
    }


    protected Viewer guessViewer() {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }


    protected SingleValueAdapter createSingleValueAdapter( SingleValueAdapter delegate ) {
        return new SingleValueAdapter() {
            @Override
            public Object getValue() {
                Object value = delegate.getValue();
                if (transformer != null) {
                    throw new RuntimeException( "not yet implemented: transformer(s)" );
                }
                if (validator != null) {
                    throw new RuntimeException( "not yet implemented: validator" );
                }
                return value;
            }
            @Override
            public void setValue( Object value ) {
                // XXX Auto-generated method stub
                throw new RuntimeException( "not yet implemented." );
            }
        };
    }

}
