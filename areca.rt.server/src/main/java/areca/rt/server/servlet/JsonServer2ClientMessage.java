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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import areca.common.base.Opt;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Color;
import areca.ui.Position;
import areca.ui.Size;
import areca.ui.component2.Events.EventHandler;
import areca.ui.component2.Property.PropertyChangedEvent;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComponent.CssStyle;
import areca.ui.component2.UIComponentEvent;
import areca.ui.component2.UIComponentEvent.ComponentAttachedEvent;
import areca.ui.component2.UIComponentEvent.ComponentDetachedEvent;
import areca.ui.component2.UIComponentEvent.ComponentEventBase;
import areca.ui.component2.UIComponentEvent.DecoratorEventBase;
import areca.ui.component2.UIComposite;
import areca.ui.component2.UIElement;

/**
 *
 * @author Falko Bräutigam
 */
public class JsonServer2ClientMessage {

    private static final Log LOG = LogFactory.getLog( JsonServer2ClientMessage.class );

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
        public Object   propNewValue;
        //public Object   propOldValue;

        public JsonUIComponentEvent( String eventType ) {
            this.eventType = eventType;
        }

        protected JsonUIComponentEvent( UIElement component ) {
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
            this( (ComponentEventBase)ev );
            this.parentId = ev.parent.id();
        }

        public JsonUIComponentEvent( ComponentDetachedEvent ev ) {
            this( (ComponentEventBase)ev );
            //this.parentId = ev.getSource().parent().id();
        }

        public JsonUIComponentEvent( DecoratorEventBase ev ) {
            this( (UIComponentEvent)ev );
            this.parentId = ev.decorated.id();
        }

        public static Opt<JsonUIComponentEvent> createFrom( PropertyChangedEvent<?> ev ) {
            var result = new JsonUIComponentEvent( (UIElement)ev.getSource().component() );
            result.eventType = ev.getClass().getSimpleName();
            result.propName = ev.getSource().name();

            if (result.propName.equals( UIComposite.PROP_COMPONENTS )
                    || result.propName.equals( UIComponent.PROP_DECORATORS )) {
                return Opt.absent();
            }
            else {
                result.propNewValue = encodeValue( ev.optNewValue().orNull() );
                //result.propOldValue = encodeValue( ev.optOldValue().orNull() );
                return result.propNewValue != null ? Opt.of( result ) : Opt.absent();
            }
        }
    }


    /**
     *
     * @return The encoded value, or null if the value can/should not be encoded.
     */
    public static JsonPropertyValueBase encodeValue( Object value ) {
        if (value == null) {
            return new JsonPropertyValueBase( "null" );
        }
        // primitive
        String type = value.getClass().getName();
        if (value instanceof String
                || value instanceof Boolean
                || value instanceof Number) {
            return new JsonPrimitivePropertyValue( type, value.toString() );
        }
        // Enum
        else if (value instanceof Enum) {
            return new JsonPrimitivePropertyValue( type, value.toString() );
        }
        // Size
        else if (value instanceof Size) {
            var s = (Size)value;
            return new JsonPrimitiveTuplePropertyValue( type, Integer.toString( s.width() ), Integer.toString( s.height() ) );
        }
        // Position
        else if (value instanceof Position) {
            var p = (Position)value;
            return new JsonPrimitiveTuplePropertyValue( type, Integer.toString( p.x() ), Integer.toString( p.y() ) );
        }
        // Color
        else if (value instanceof Color) {
            var c = (Color)value;
            return new JsonPrimitivePropertyValue( type, c.toHex() );
        }
        // EventHandler
        else if (value instanceof EventHandler) {
            return new JsonPrimitivePropertyValue( "EventHandler", ((EventHandler)value).type.toString() );
        }
        // CssStyle
        else if (value instanceof CssStyle) {
            var cssStyle = (CssStyle)value;
            return new JsonPrimitiveTuplePropertyValue( "CssStyle", cssStyle.name, cssStyle.value );
        }
        // Collection
        else if (value instanceof Collection) {
            var result = new JsonCollectionPropertyValue( "collection" );
            for (var v : (Collection)value) {
                var encoded = encodeValue( v );
                if (encoded == null) {
                    return encoded;
                }
                else {
                    result.values.add( encoded );
                }
            }
            return result;
        }
        else {
            LOG.debug( "Value type missing: %s", value.getClass().getSimpleName() );
            return null; //new JsonPropertyValueBase( "missing" );
        }
    }

    /**
     *
     */
    public static class JsonPropertyValueBase {

        public static final JsonPropertyValueBase MISSING = new JsonPropertyValueBase( "missing" );

        public String   type;

        protected JsonPropertyValueBase( String type ) {
            this.type = type;
        }
    }

    /**
     *
     */
    public static class JsonCollectionPropertyValue
            extends JsonPropertyValueBase {

        public List<Object> values = new ArrayList<>();

        protected JsonCollectionPropertyValue( String type ) {
            super( type );
        }
    }

    /**
     *
     */
    public static class JsonPrimitivePropertyValue
            extends JsonPropertyValueBase {

        public String   value;

        protected JsonPrimitivePropertyValue( String type, String value ) {
            super( type );
            this.value = value;
        }
    }

    /**
     *
     */
    public static class JsonPrimitiveTuplePropertyValue
            extends JsonPrimitivePropertyValue {

        public String   value2;

        protected JsonPrimitiveTuplePropertyValue( String type, String value, String value2 ) {
            super( type, value );
            this.value2 = value2;
        }
    }
}