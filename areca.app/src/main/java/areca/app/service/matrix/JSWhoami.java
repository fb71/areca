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

/**
 *
 */
public abstract class JSWhoami
        implements JSCommon {

    @JSProperty( "user_id" )
    public abstract String getUserId();

    @JSProperty( "is_guest" )
    public abstract boolean isGuest();

    @JSProperty( "device_id" )
    public abstract OptString deviceId();

    @Override
    public String toString() {
        return String.format( "Whoami[getUserId()=%s, isGuest()=%s, deviceId()=%s]", getUserId(), isGuest(), deviceId() );
    }
}