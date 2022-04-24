/*
 * Copyright (C) 2020, the @authors. All rights reserved.
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
package areca.common.event;

import java.util.EventObject;

/**
 * Simple {@link EventManager} implementation that executes event handlers right in
 * the same thread/stack of the {@link #publish(java.util.EventObject)} call.
 *
 * @author Falko Bräutigam
 */
public class SameStackEventManager
        extends EventManager {

    @Override
    public /*Promise<Void>*/ void publish( EventObject ev ) {
        fireEvent( ev );
//        return new Promise.Completable<>() {{
//            complete( null );
//        }};
    }


    @Override
    public void publishAndWait( EventObject ev ) {
        publish( ev );
    }

}
