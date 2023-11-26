/*
 * Copyright (C) 2023, the @authors. All rights reserved.
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
package areca.rt.server.servlet;

import java.util.List;
import java.util.stream.Stream;

import areca.rt.server.client.PropertyValueCoder;
import areca.ui.component2.Property.PropertyChangedEvent;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComponentEvent;
import areca.ui.component2.UIComponentEvent.ComponentAttachedEvent;
import areca.ui.component2.UIComponentEvent.ComponentDetachedEvent;

/**
 *
 * @author Falko Bräutigam
 */
class JsonServer2ClientMessage {

    public List<JsonUIComponentEvent> uiEvents;

    public int pendingWait;

    /**
     *
     */
    public static class JsonUIComponentEvent {
        public String   eventType;
        public Integer  componentId;
        public String   componentClass;
        public Integer  parentId;

        public String   propName;
        public String   propValueType;
        public String   propNewValue;
        public String   propOldValue;

        protected JsonUIComponentEvent( UIComponent component ) {
            this.componentId = component.id();
            this.componentClass = Stream.iterate( (Class)component.getClass(), cl -> cl.getSuperclass() )
                    .filter( cl -> !cl.isAnonymousClass() )
                    .map( cl -> cl.getName() )
                    .filter( cln -> cln.startsWith( "areca.ui" ) )
                    .findFirst().orElseThrow( () -> new RuntimeException( "No 'areca.ui' parent found: " + component.getClass() ) );
        }

        public JsonUIComponentEvent( UIComponentEvent ev ) {
            this( ev.getSource() );
            this.eventType = ev.getClass().getSimpleName();
        }

        public JsonUIComponentEvent( ComponentAttachedEvent ev ) {
            this( (UIComponentEvent)ev );
            this.parentId = ev.parent.id();
        }

        public JsonUIComponentEvent( ComponentDetachedEvent ev ) {
            this( (UIComponentEvent)ev );
            //this.parentId = ev.getSource().parent().id();
        }

        public JsonUIComponentEvent( PropertyChangedEvent<?> ev ) {
            this( (UIComponent)ev.getSource().component() );
            this.eventType = ev.getClass().getSimpleName();
            this.propName = ev.getSource().name();
            this.propValueType = ev.optNewValue().map( v -> v.getClass().getName() ).orElse( "null" );
            this.propNewValue = PropertyValueCoder.encode( ev.getSource(), ev.optNewValue().orNull() );
            this.propOldValue = PropertyValueCoder.encode( ev.getSource(), ev.optOldValue().orNull() );
        }
    }
}