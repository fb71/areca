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

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/**
 * Client to server message.
 */
abstract class JSClient2ServerMessage
        implements JSObject {

    @JSBody( script = "return {};" )
    public static native JSClient2ServerMessage create();

    @JSProperty("startSession")
    public abstract void setStartSession( boolean v );

    @JSProperty("events")
    public abstract void setEvents( JSClient2ServerMessage.JSClickEvent[] v );

    /**
     *
     */
    public static abstract class JSClickEvent
            implements JSObject {

        @JSBody( script = "return {};" )
        public static native JSClient2ServerMessage.JSClickEvent create();

        @JSProperty("eventType")
        public abstract void setEventType( String v );

        @JSProperty("position")
        public abstract void setPosition( String v );

        @JSProperty("componentId")
        public abstract void setComponentId( int v );
    }
}