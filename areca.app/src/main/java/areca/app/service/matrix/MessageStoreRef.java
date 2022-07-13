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

import areca.app.model.MatrixSettings;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 */
class MessageStoreRef
        extends MatrixStoreRef {

    private static final Log LOG = LogFactory.getLog( MessageStoreRef.class );

    /** Decode */
    public MessageStoreRef() { }

    public MessageStoreRef( MatrixSettings settings, JSEvent event ) {
        this( settings, event.roomId(), event.eventId() );
    }

    public MessageStoreRef( MatrixSettings settings, String roomId, String eventId ) {
        super( settings );
        parts.add( roomId );
        parts.add( eventId );
    }

    @Override
    public String prefix() {
        return super.prefix() + "msg";
    }

    public String roomId() {
        return parts.get( 1 );
    }

    public String eventId() {
        return parts.get( 2 );
    }
}
