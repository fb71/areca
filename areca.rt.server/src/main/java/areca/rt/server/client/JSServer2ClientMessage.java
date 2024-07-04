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
package areca.rt.server.client;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Position;
import areca.ui.Size;
import areca.ui.component2.Button;
import areca.ui.component2.DatePicker;
import areca.ui.component2.Events.EventType;
import areca.ui.component2.Text;
import areca.ui.component2.TextField;
import areca.ui.component2.UIComponent.CssStyle;

/**
 *
 */
abstract class JSServer2ClientMessage
        implements JSObject {

    private static final Log LOG = LogFactory.getLog( JSServer2ClientMessage.class );

    public static final Object  VALUE_MISSING = new Object();

    @JSProperty("uiEvents")
    public abstract JSServer2ClientMessage.JSUIComponentEvent[] uiEvents();

    @JSProperty("pendingWait")
    public abstract int pendingWait();

    /**
     *
     */
    public static abstract class JSUIComponentEvent
            implements JSObject {

        @JSProperty("eventType")
        public abstract String eventType();

        @JSProperty("componentId")
        public abstract int componentId();

        @JSProperty("componentClass")
        public abstract String componentClass();

        @JSProperty("parentId")
        public abstract int parentId();

        @JSProperty("propName")
        public abstract String propName();

//        @JSProperty("propValueType")
//        public abstract String propValueType();

        @JSProperty("propNewValue")
        public abstract JSObject propNewValue();

        @JSProperty("propOldValue")
        public abstract JSObject propOldValue();
    }


    /**
     * Decode property value encoded by JsonServer2ClientMessage.
     */
    public static Object decodeValue( JSPropertyValueBase value ) {
        if (value.type().equals( "missing" )) {
            return VALUE_MISSING;
        }
        else if (value.type().equals( "null" )) {
            return null;
        }
        // String
        else if (value.type().equals( String.class.getName() )) {
            var primitive = (JSPrimitivePropertyValue)value;
            return primitive.value();
        }
        // Integer
        else if (value.type().equals( Integer.class.getName() )) {
            var primitive = (JSPrimitivePropertyValue)value;
            return Integer.valueOf( primitive.value() );
        }
        // Boolean
        else if (value.type().equals( Boolean.class.getName() )) {
            var primitive = (JSPrimitivePropertyValue)value;
            return Boolean.parseBoolean( primitive.value() );
        }
        // Enum: Text.Format
        else if (value.type().equals( Text.Format.class.getName() )) {
            var primitive = (JSPrimitivePropertyValue)value;
            return Text.Format.valueOf( primitive.value() );
        }
        // Enum: Button.Type
        else if (value.type().equals( Button.Type.class.getName() )) {
            var primitive = (JSPrimitivePropertyValue)value;
            return Button.Type.valueOf( primitive.value() );
        }
        // Enum: TextField.Type
        else if (value.type().equals( TextField.Type.class.getName() )) {
            var primitive = (JSPrimitivePropertyValue)value;
            return TextField.Type.valueOf( primitive.value() );
        }
        // Enum: DatePicker.DateTime
        else if (value.type().equals( DatePicker.DateTime.class.getName() )) {
            var primitive = (JSPrimitivePropertyValue)value;
            return DatePicker.DateTime.valueOf( primitive.value() );
        }
        // Size
        else if (value.type().equals( Size.class.getName() )) {
            var primitive = (JSPrimitivePropertyValue)value;
            return Size.of( Integer.parseInt( primitive.value() ), Integer.parseInt( primitive.value2() ) );
        }
        // Position
        else if (value.type().equals( Position.class.getName() )) {
            var primitive = (JSPrimitivePropertyValue)value;
            return Position.of( Integer.parseInt( primitive.value() ), Integer.parseInt( primitive.value2() ) );
        }
        // CssStyle
        else if (value.type().equals( "CssStyle" )) {
            var primitive = (JSPrimitivePropertyValue)value;
            return CssStyle.of( primitive.value(), primitive.value2() );
        }
        // EventType
        else if (value.type().equals( "EventHandler" )) {
            var primitive = (JSPrimitivePropertyValue)value;
            return EventType.valueOf( primitive.value() );
        }
        // collection
        else if (value.type().equals( "collection" )) {
            var coll = (JSCollectionPropertyValue)value;
            return Sequence.of( coll.values() ).map( v -> decodeValue( v ) ).toList();
        }
        else {
            throw new RuntimeException( "UNHANDLED: type = " + value.type() );
        }
    }


    /**
     *
     */
    public static abstract class JSPropertyValueBase
            implements JSObject {

        @JSProperty("type")
        public abstract String type();
    }


    /**
     *
     */
    public static abstract class JSPrimitivePropertyValue
            extends JSPropertyValueBase {

        @JSProperty("value")
        public abstract String value();

        @JSProperty("value2")
        public abstract String value2();
    }


    /**
     *
     */
    public static abstract class JSCollectionPropertyValue
            extends JSPropertyValueBase {

        @JSProperty("values")
        public abstract JSPropertyValueBase[] values();
    }

}