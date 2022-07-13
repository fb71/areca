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
package areca.app.service.matrix;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSProperty;

/**
 * Timeline event. Result when accessing the store.
 */
public interface JSStoredEvent
        extends JSCommon<JSStoredEvent> {

    @JSProperty("event_id")
    public String eventId();

    @JSProperty("room_id")
    public String roomId();

    /** Like "m.room.message" or "m.room.encrypted" */
    @JSProperty("type")
    public String type();

    /** The Matrix address of the sender. */
    @JSProperty("sender")
    public String sender();

//    @Override
    @JSBody(params = {}, script = "return Date.now() - this.unsigned.age;")
    public double date();

    @JSBody(params = {}, script = "return this.unsigned.age;")
    public int age();

    @JSProperty("content")
    public JSCommon content();

    public default String toString2() {
        return String.format( "Event[type=%s, sender=%s]", type(), sender() );
    }

    public default ContentEvent asContentEvent() {
        return new ContentEvent() {
            @Override public String eventId() {
                return JSStoredEvent.this.eventId();
            }
            @Override public String roomId() {
                return JSStoredEvent.this.roomId();
            }
            @Override public String type() {
                return JSStoredEvent.this.type();
            }
            @Override public String sender() {
                return JSStoredEvent.this.sender();
            }
            @Override public JSCommon content() {
                return JSStoredEvent.this.content();
            }
            @Override public long date() {
                return (long)JSStoredEvent.this.date();
            }
        };
    }

}