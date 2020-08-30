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
package areca.rt.teavm;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.logging.Logger;

import org.teavm.jso.browser.Window;

import areca.common.event.EventManager;

/**
 *
 * @author Falko Bräutigam
 */
public class SetTimeoutEventManager
        extends EventManager {

    private static final Logger LOG = Logger.getLogger( SetTimeoutEventManager.class.getSimpleName() );

    private static final int        INIT_QUEUE_CAPACITY = 256;

    private volatile int            timeoutId = -1;

    private List<EventObject>       eventQueue = new ArrayList<>( INIT_QUEUE_CAPACITY );


    @Override
    public void publish( EventObject ev ) {
        eventQueue.add( ev );

        if (timeoutId == -1) {
            timeoutId = Window.setTimeout( () -> {
                synchronized (this) {
                    LOG.info( "eventQueue: " + eventQueue.size() );
                    timeoutId = -1;
                    List<EventObject> stable = eventQueue;
                    eventQueue = new ArrayList<>( INIT_QUEUE_CAPACITY );
                    for (EventObject cursor : stable) {
                        fireEvent( cursor );
                    }
                    notifyAll();
                }
            }, 0);
        }
        else {
            if (eventQueue.size() >= 1000) {
                waitForPendingAsyncEvents();
            }
        }
    }


    @Override
    public void publishAndWait( EventObject ev ) {
        waitForPendingAsyncEvents();

        fireEvent( ev );
    }


    protected void waitForPendingAsyncEvents() {
        if (timeoutId != -1) {
            synchronized (this) {
                while (timeoutId != -1) {
                    try {
                        wait();
                    }
                    catch (InterruptedException e) {
                    }
                }
            }
        }
    }

}
