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

/**
 *
 */
abstract class JSServer2ClientMessage implements JSObject {

    @JSProperty("uiEvents")
    public abstract JSServer2ClientMessage.JSUIComponentEvent[] uiEvents();

    @JSProperty("pendingWait")
    public abstract int pendingWait();

    /**
     *
     */
    public static abstract class JSUIComponentEvent implements JSObject {

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

        @JSProperty("propValueType")
        public abstract String propValueType();

        @JSProperty("propNewValue")
        public abstract String propNewValue();

        @JSProperty("propOldValue")
        public abstract String propOldValue();
    }
}