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
package areca.ui.modeladapter;

import areca.common.event.EventManager;
import areca.common.event.EventManager.EventHandlerInfo;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public abstract class ModelValueBase {

    private static final Log LOG = LogFactory.getLog( ModelValueBase.class );

    private boolean disposed;

    public void dispose() {
        this.disposed = true;
    }

    public boolean isDisposed() {
        return disposed;
    }

    public EventHandlerInfo onChange( Object annotated ) {
        return EventManager.instance().subscribe( annotated )
                .performIf( ModelChangeEvent.class, ev -> ev.getSource() == ModelValueBase.this )
                .unsubscribeIf( () -> isDisposed() );
    }

    public EventHandlerInfo onChange( ModelChangeListener l ) {
        return EventManager.instance().subscribe( l )
                .performIf( ModelChangeEvent.class, ev -> ev.getSource() == ModelValueBase.this )
                .unsubscribeIf( () -> isDisposed() );
    }

    protected void fireChangeEvent() {
        EventManager.instance().publish( new ModelChangeEvent( this ) );
    }

    /**
     * Registers a listener for the given other ModelValue so that a change
     * on other triggers an event on this ModelValue.
     */
    protected void fireIfChanged( ModelValueBase other ) {
        other.onChange( ev -> fireChangeEvent() )
                .unsubscribeIf( () -> isDisposed() );
    }
}
