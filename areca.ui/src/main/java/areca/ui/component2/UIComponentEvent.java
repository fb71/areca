/*
 * Copyright (C) 2022, the @authors. All rights reserved.
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
package areca.ui.component2;

import java.util.EventObject;

import areca.common.Session;
import areca.common.event.EventManager;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.Property.PropertyChangedEvent;

/**
 *
 * @author Falko Bräutigam
 */
public abstract class UIComponentEvent
        extends EventObject {

    private static final Log LOG = LogFactory.getLog( UIComponentEvent.class );

    /**
     * Component lifecycle and {@link PropertyChangedEvent}s. For internal use
     * while rendering.
     *
     * XXX replace with EventHandlers
     */
    public static EventManager manager() {
        return Session.instanceOf( UIEventManager.class );
    }


    /** */
    public static class ComponentConstructedEvent
            extends UIComponentEvent {

        public ComponentConstructedEvent( UIComponent source ) {
            super( source );
        }
    }

    /** */
    public static class ComponentAttachedEvent
            extends UIComponentEvent {

        public UIComposite parent;

        public ComponentAttachedEvent( UIComponent source, UIComposite parent ) {
            super( source );
            this.parent = parent;
        }
    }

    /** */
    public static class ComponentDetachedEvent
            extends UIComponentEvent {

        public ComponentDetachedEvent( UIComponent source ) {
            super( source );
        }
    }

    /** */
    public static class ComponentDisposedEvent
            extends UIComponentEvent {

        public ComponentDisposedEvent( UIComponent source ) {
            super( source );
        }
    }

    // instance *******************************************

    public UIComponentEvent( UIComponent source ) {
        super( source );
    }

    @Override
    public UIComponent getSource() {
        return (UIComponent)super.getSource();
    }

    @Override
    public String toString() {
        return String.format( "%s[%s]", getClass().getSimpleName(), getSource().getClass().getSimpleName() );
    }

}
