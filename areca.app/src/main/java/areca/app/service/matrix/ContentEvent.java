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

import areca.common.base.Opt;

/**
 * Unified interface for {@link JSEvent} and {@link JSStoredEvent}.
 *
 * @author Falko Bräutigam
 */
public interface ContentEvent {

    public String eventId();

    public String roomId();

    public String type();

    public long date();

    public String sender();

    public JSCommon content();

    public default Opt<JSMessage> messageContent()  {
        return type().equals( "m.room.message" ) ? Opt.of( content().cast() ) : Opt.absent();
    }

    public default Opt<JSEncrypted> encryptedContent()  {
        return type().equals( "m.room.encrypted" ) ? Opt.of( content().cast() ) : Opt.absent();
    }

}
