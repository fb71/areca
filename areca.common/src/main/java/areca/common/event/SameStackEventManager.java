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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Simple {@link EventManager} implementation that executes event handlers right in
 * the same thread/stack of the {@link #publish(java.util.EventObject)} call.
 *
 * @author Falko Bräutigam
 */
public class SameStackEventManager
        extends EventManager {

    private static final Logger LOG = Logger.getLogger( SameStackEventManager.class.getName() );

    private Map<Integer,EventHandlerInfo>      handlers = new HashMap<>( 256 );


    protected Integer keyOf( Object annotatedOrListener ) {
        return Integer.valueOf( System.identityHashCode( annotatedOrListener ) );
    }


    @Override
    public EventHandlerInfo subscribe( Object annotatedOrListener ) {
        EventHandlerInfo info = new EventHandlerInfo( annotatedOrListener );
        if (handlers.put( keyOf( annotatedOrListener ), info ) != null) {
            throw new IllegalStateException( "Event handler already subscribed!" );
        }
        return info;
    }


    @Override
    protected void unsubscribe( EventHandlerInfo eventHandlerInfo ) {
        if (handlers.remove( keyOf( eventHandlerInfo.handler ) ) == null) {
            throw new IllegalStateException( "Event handler was not subscribed!" );
        }
    }


    @Override
    public void publish( EventObject ev ) {
        for (EventHandlerInfo handler : handlers.values()) {
            handler.perform( ev );
        }
    }


    @Override
    public void publishAndWait( EventObject ev ) {
        publish( ev );
    }

}
