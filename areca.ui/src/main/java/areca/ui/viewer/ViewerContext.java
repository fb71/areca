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

import static areca.ui.viewer.model.ModelBase.VALID;

import areca.common.Assert;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.layout.FillLayout;
import areca.ui.layout.RowConstraints;
import areca.ui.viewer.model.ModelBase;
import areca.ui.viewer.model.ModelBase.ValidationResult;
import areca.ui.viewer.transform.ValidatingModel;

/**
 * The context of a {@link Viewer} that puts together the viewer, model and
 * validation.
 *
 * @author Falko Bräutigam
 */
public class ViewerContext<M extends ModelBase>
        implements ViewerBuilder<M> {

    private static final Log LOG = LogFactory.getLog( ViewerContext.class );

    protected Viewer<M>     viewer;

    protected ModelBase     model;

    protected boolean       isChanged;

    protected ValidationResult validationResult = VALID;


    @Override
    @SuppressWarnings({"hiding", "unchecked"})
    public <R extends M> ViewerBuilder<R> viewer( Viewer<R> viewer ) {
        this.viewer = (Viewer<M>)viewer;
        return (ViewerBuilder<R>)this;
    }


    @Override
    @SuppressWarnings("hiding")
    public ViewerBuilder<M> model( M model ) {
        this.model = model;
        return this;
    }


    @Override
    @SuppressWarnings("unchecked")
    public UIComponent create() {
        viewer = viewer != null ? viewer : guessViewer();
        Assert.notNull( model, "Call ViewerBuilder#model() before create()!" );

        viewer.init( (M)model );

        // listen to UI input
        viewer.subscribe( ev -> {
            isChanged = true;
            validationResult = model instanceof ValidatingModel
                    ? ((ValidatingModel<Object>)model).validate( ev.newValue )
                    : VALID;
        });

        var field = viewer.create();

        // container for label and valid decorators
        return new UIComposite() {{
            // FIXME hack! UIComposite does not calculate its minHeight depending on its children (yet:)
            layoutConstraints.set( new RowConstraints().height.set( field.computeMinHeight( 400 ) ) );
            layout.set( new FillLayout() );
            add( field );
        }};
    }


    public boolean isValid() {
        return validationResult == VALID;
    }


    public boolean isChanged() {
        return isChanged;
    }


    protected Viewer<M> guessViewer() {
        throw new RuntimeException( "not yet implemented." );
    }

}
