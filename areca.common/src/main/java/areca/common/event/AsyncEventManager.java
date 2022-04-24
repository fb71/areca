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

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import areca.common.Platform;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * Not yet tested.
 *
 * @author Falko Bräutigam
 */
public class AsyncEventManager
        extends EventManager {

    private static final Log LOG = LogFactory.getLog( AsyncEventManager.class );

    private static final int        INIT_QUEUE_CAPACITY = 64;

    private List<EventObject>       eventQueue = null;


    @Override
    public void publish( EventObject ev ) {
        if (eventQueue == null) {
            eventQueue = new ArrayList<>( INIT_QUEUE_CAPACITY );

            Platform.async( () -> {
                LOG.warn( "Queue: " + eventQueue.size() );
                for (EventObject cursor : eventQueue) {
                    fireEvent( cursor );
                }
                eventQueue = null;

                synchronized (this) {
                    notifyAll();
                }
            });
        }
        eventQueue.add( ev );
    }


    @Override
    public void publishAndWait( EventObject ev ) {
        if (eventQueue != null) {
            synchronized (this) {
                try {
                    wait();
                }
                catch (InterruptedException e) {
                    LOG.warn( "wait() interrupted!", e );
                }
            }
        }
        fireEvent( ev );
    }

}
