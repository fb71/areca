/*
 * Copyright (C) 2019, the @authors. All rights reserved.
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
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.logging.Logger;

/**
 * @author falko
 *
 */
public class EventManager {

    private static final Logger LOG = Logger.getLogger( EventManager.class.getSimpleName() );

    private static final EventManager   INSTANCE = new EventManager();

    public static EventManager instance() {
        return INSTANCE;
    }

    // instance *******************************************

    private Thread                  handlerThread;

    private Queue<Execution>        queue = new LinkedList<>();

    private List<EventListener<?>>  listeners = new ArrayList<>();


    private class Execution {

    }


    protected EventManager() {
        handlerThread = new Thread( () -> {
            for (;;) {

            }
        });
    }


    public EventManager subscribe( EventListener<?> l ) {
        listeners.add( l );
        return this;
    }


    public void publish( EventObject ev ) {
        for (EventListener l : listeners) {
            l.handle( ev );
        }
    }

}
