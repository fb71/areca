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

import java.util.Objects;

import areca.common.Assert;
import areca.common.Platform;
import areca.common.event.EventListener;
import areca.common.event.EventManager;
import areca.common.event.EventManager.EventHandlerInfo;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.layout.FillLayout;
import areca.ui.viewer.Viewer.ViewerInputChangeEvent;
import areca.ui.viewer.model.ModelBase;
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

    protected Object        loadedValue;

    protected Object        currentValue;


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
            LOG.debug( "new value: '%s'", ev.newValue );
            currentValue = ev.newValue;
        });

        var field = viewer.create();

        // container for label and valid decorators
        return new UIComposite() {{
            // FIXME hack! UIComposite does not calculate its minHeight depending on its children (yet:)
            //layoutConstraints.set( new RowConstraints().height.set( field.computeMinHeight( 400 ) ) );
            layout.set( new FillLayout() );
            add( field );
        }};
    }


    @Override
    public UIComponent createAndLoad() {
        var result = create();
        load();
        return result;
    }


    @Override
    public EventHandlerInfo subscribe( EventListener<ViewerInputChangeEvent> l ) {
        return EventManager.instance()
                .subscribe( ev -> {
                    // XXX ViewerBuilder.subscribe() might be called *before* the viewer is initialized
                    // so we must wait Viewers have processed their event handlers, in order to
                    // see correct results for isChanged() and isValid()
                    Platform.async( () -> l.handle( (ViewerInputChangeEvent)ev ) );
                })
                .performIf( ViewerInputChangeEvent.class, ev -> {
                    return viewer == ev.getSource();
                });
    }


    public void store() {
        loadedValue = currentValue = viewer.store();
    }

    public void load() {
        loadedValue = currentValue = viewer.load();
        //LOG.debug( "load: '%s'", StringUtils.abbreviate( loadedValue.toString(), 20 ) );
    }


    @SuppressWarnings( "unchecked" )
    public boolean isValid() {
        if (model instanceof ValidatingModel) {
            return ((ValidatingModel<Object>)model).validate( currentValue ) == VALID;
        }
        return true;
    }


    public boolean isChanged() {
        LOG.debug( "isChanged: '%s' - '%s'", loadedValue, currentValue );
        return !Objects.equals( loadedValue, currentValue );
    }


    protected Viewer<M> guessViewer() {
        throw new RuntimeException( "not yet implemented." );
    }

}
