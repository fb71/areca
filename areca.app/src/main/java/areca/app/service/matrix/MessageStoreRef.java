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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import areca.common.base.Opt;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 */
class MessageStoreRef {

    private static final Log LOG = LogFactory.getLog( MessageStoreRef.class );

    public static final Pattern pattern = Pattern.compile( "matrix:([^|]*)\\|(.*)" );

    public String roomId;

    public String eventId;


    public static MessageStoreRef of( String _roomId, String _eventId ) {
        return new MessageStoreRef() {{ this.roomId = _roomId; this.eventId = _eventId; }};
    }


    public static Opt<MessageStoreRef> parse( String storeRef ) {
        Matcher matcher = pattern.matcher( storeRef );
        if (!matcher.matches()) {
            LOG.info( "storeRef pattern does not match: " + storeRef );
        }
        return Opt.of( matcher.matches()
                ? new MessageStoreRef() {{
                    this.roomId = matcher.group( 1 );
                    this.eventId = matcher.group( 2 ); }}
                : null);
    }


    public String toString() {
        return String.format( "matrix:%s|%s", roomId, eventId );
    }
}
