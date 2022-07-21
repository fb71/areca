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

import areca.app.service.matrix.JSEvent.EventType;

/**
 *
 */
public abstract class JSMessage
        implements JSCommon<JSMessage> {

    @JSBody(params = {}, script = "return {};")
    public static native JSMessage create();

    public static JSMessage create( String msgType, String body ) {
        var result = create();
        result.setMsgtype( msgType );
        result.setBody( body );
        return result;
    }

    @JSProperty( "msgtype" )
    public abstract OptString msgtype();

    public boolean isMsgtype( EventType check ) {
        return check.equals( msgtype().opt().orElse( null ) );
    }

    @JSProperty
    public abstract void setMsgtype( String type );

    @JSProperty( "body" )
    public abstract OptString body();

    @JSProperty
    public abstract void setBody( String body );

    @JSProperty
    public abstract OptString getFormat();
}