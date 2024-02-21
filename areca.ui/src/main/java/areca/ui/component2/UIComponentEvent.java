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
     * Component lifecycle (and {@link PropertyChangedEvent}s). For internal use
     * while rendering.
     *
     * XXX replace with EventHandlers
     */
    public static EventManager manager() {
        return Session.instanceOf( UIEventManager.class );
    }


    /** */
    public static class ComponentConstructingEvent
            extends UIComponentEvent {

        public ComponentConstructingEvent( UIElement source ) {
            super( source );
        }
    }

    /** */
    public static class ComponentConstructedEvent
            extends UIComponentEvent {

        public ComponentConstructedEvent( UIElement source ) {
            super( source );
        }
    }

    /** */
    public static abstract class ComponentEventBase
            extends UIComponentEvent {

        public ComponentEventBase( UIComponent source ) {
            super( source );
        }

        @Override
        public UIComponent getSource() {
            return (UIComponent)super.getSource();
        }
    }

    /** */
    public static class ComponentAttachedEvent
            extends ComponentEventBase {

        public UIComposite parent;

        public ComponentAttachedEvent( UIComponent source, UIComposite parent ) {
            super( source );
            this.parent = parent;
        }
    }

    /** */
    public static class ComponentDetachedEvent
            extends ComponentEventBase {

        public ComponentDetachedEvent( UIComponent source ) {
            super( source );
        }
    }

    /** */
    public static abstract class DecoratorEventBase
            extends UIComponentEvent {

        public UIComponent decorated;

        public DecoratorEventBase( UIComponentDecorator source, UIComponent decorated ) {
            super( source );
            this.decorated = decorated;
        }

        @Override
        public UIComponentDecorator getSource() {
            return (UIComponentDecorator)super.getSource();
        }
    }

    /** */
    public static class DecoratorAttachedEvent
            extends DecoratorEventBase {

        public DecoratorAttachedEvent( UIComponentDecorator source, UIComponent decorated ) {
            super( source, decorated );
        }
    }

    /** */
    public static class DecoratorDetachedEvent
            extends DecoratorEventBase {

        public DecoratorDetachedEvent( UIComponentDecorator source, UIComponent decorated ) {
            super( source, decorated );
        }
    }

    /** */
    public static class ComponentDisposedEvent
            extends UIComponentEvent {

        public ComponentDisposedEvent( UIElement source ) {
            super( source );
        }
    }

    // instance *******************************************

    /**
     * @param source {@link UIComponent} or {@link UIComponentDecorator}.
     */
    public UIComponentEvent( UIElement source ) {
        super( source );
    }

    @Override
    public UIElement getSource() {
        return (UIElement)super.getSource();
    }

    @Override
    public String toString() {
        return String.format( "%s[%s]", getClass().getSimpleName(), getSource().getClass().getSimpleName() );
    }

}
