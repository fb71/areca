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

import areca.common.base.Opt;

/**
 *
 */
public interface JSEvent
        extends JSCommon<JSEvent> {

    /**
     *
     */
    enum EventType {
        M_ROOM_MESSAGE, M_ROOM_ENCRYPTED;

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

    public default Opt<JSMessage> messageContent()  {
        return type().equals( "m.room.message" ) ? Opt.of( content().cast() ) : Opt.absent();
    }

    public default Opt<JSEncrypted> encryptedContent()  {
        return type().equals( "m.room.encrypted" ) ? Opt.of( content().cast() ) : Opt.absent();
    }

    public default String toString2() {
        return String.format( "Event[type=%s, sender=%s]", type(), sender() );
    }
}
