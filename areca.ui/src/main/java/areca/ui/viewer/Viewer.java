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

import java.util.EventObject;

import areca.common.Assert;
import areca.common.event.EventListener;
import areca.common.event.EventManager;
import areca.common.event.EventManager.EventHandlerInfo;
import areca.ui.component2.UIComponent;
import areca.ui.viewer.model.ModelBase;

/**
 *
 * @author Falko Bräutigam
 */
public abstract class Viewer<M extends ModelBase> {

    protected M         model;

    @SuppressWarnings("hiding")
    protected Viewer<M> init( M model ) {
        this.model = Assert.notNull( model );
        return this;
    }

    public abstract UIComponent create();

    public abstract void store();

    public abstract void load();


    /**
     * True if the underlying {@link UIComponent#isDisposed()}.
     */
    protected abstract boolean isDisposed();

    /**
     * Subscribes a listener this receives a {@link ViewerInputChangeEvent} when the
     * user has changed the value in the UI.
     */
    public EventHandlerInfo subscribe( EventListener<ViewerInputChangeEvent> l ) {
        return EventManager.instance().subscribe( l )
                .performIf( ViewerInputChangeEvent.class, ev -> ev.getSource() == Viewer.this )
                .unsubscribeIf( () -> isDisposed() );
    }

    /**
     * Fires a {@link ViewerInputChangeEvent}.
     */
    protected void fireEvent( Object newValue, Object oldValue) {
        EventManager.instance().publish( new ViewerInputChangeEvent( this, newValue, oldValue ) );
    }


    /**
     * Signals that the user has changed the value in the UI.
     */
    public static class ViewerInputChangeEvent
            extends EventObject {

        public Object newValue;

        public Object oldValue;

        public ViewerInputChangeEvent( Viewer<?> source, Object newValue, Object oldValue ) {
            super( source );
            this.newValue = newValue;
            this.oldValue = oldValue;
        }
    }
}
