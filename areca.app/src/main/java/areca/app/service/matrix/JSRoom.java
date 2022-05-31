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
public interface JSRoom
        extends JSCommon<JSRoom> {

    @JSProperty("roomId")
    public String roomId();

    @JSProperty("name")
    public String name();

    @JSProperty("timeline")
    public JSTimeline[] timeline();

    public default String toString2() {
        return String.format( "Room[name=%s, roomId=%s]", name(), roomId() );
    }
}