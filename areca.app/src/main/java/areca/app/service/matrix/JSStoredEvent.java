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

import org.teavm.jso.JSProperty;

import areca.common.base.Opt;

/**
 *
 */
public interface JSStoredEvent
        extends JSCommon<JSStoredEvent> {

    @JSProperty("event_id")
    public String eventId();

    @JSProperty("type")
    public String type();

    @JSProperty("sender")
    public String sender();

    @JSProperty("content")
    public JSCommon content();

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