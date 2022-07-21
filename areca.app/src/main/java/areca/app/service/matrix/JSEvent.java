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

import org.teavm.jso.JSMethod;
import org.teavm.jso.core.JSDate;

/**
 * Fired by the main event loop of the {@link MatrixClient Matrix client}.
 */
public interface JSEvent
        extends JSCommon<JSEvent> {

    /**
     *
     */
    enum EventType {
        M_ROOM_MESSAGE, M_ROOM_ENCRYPTED, M_BAD_ENCRYPTED;

        //@Override
        public boolean equals( String other ) {
            return toString().equals( other );
        }

        @Override
        public String toString() {
            return super.toString().toLowerCase().replace( "_", "." );
        }
    }

    @JSMethod("getId")
    public String eventId();

    @JSMethod("getRoomId")
    public String roomId();

    @JSMethod("getType")
    public String type();

//    @Override
    @JSMethod("getDate")
    public JSDate date();

    @JSMethod("getSender")
    public String sender();

    @JSMethod("getContent")
    public JSCommon content();

    /**
      * https://github.com/matrix-org/matrix-js-sdk/issues/1767
      */
    @JSMethod("getWiredContent")
    public JSCommon wiredContent();

    public default String toString2() {
        return String.format( "Event[type=%s, sender=%s]", type(), sender() );
    }

    public default boolean isType( EventType check ) {
        return check.equals( type() );
    }

    public default ContentEvent asContentEvent() {
        return new ContentEvent() {
            @Override public String eventId() {
                return JSEvent.this.eventId();
            }
            @Override public String roomId() {
                return JSEvent.this.roomId();
            }
            @Override public String type() {
                return JSEvent.this.type();
            }
            @Override public String sender() {
                return JSEvent.this.sender();
            }
            @Override public JSCommon content() {
                return JSEvent.this.content();
            }
            @Override public long date() {
                return (long)JSEvent.this.date().getTime();
            }
        };
    }
}
