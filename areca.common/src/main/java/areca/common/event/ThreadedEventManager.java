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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;


/**
 *
 * @author Falko Bräutigam
 */
public class ThreadedEventManager
        extends EventManager {

    private static final Logger LOG = Logger.getLogger( ThreadedEventManager.class.getSimpleName() );

    private EventThread                 eventThread = new EventThread();

    private BlockingQueue<EventObject>  eventQueue = new ArrayBlockingQueue<>( 1000 );


    public ThreadedEventManager() {
        eventThread.start();
    }


    @Override
    public void publish( EventObject ev ) {
        try {
            //System.out.println( "QUEUE: " + eventQueue.size() );
            eventQueue.put( ev );
        }
        catch (InterruptedException e) {
            throw new RuntimeException( e );
        }
    }


    @Override
    public void publishAndWait( EventObject ev ) {
        synchronized (ev) {
            publish( ev );
            try {
                ev.wait();
            }
            catch (InterruptedException e) {
                throw new RuntimeException( e );
            }
        }
    }


    /**
     *
     */
    class EventThread extends Thread {

        protected EventThread() {
            setPriority( MIN_PRIORITY );
        }

        @Override
        public void run() {
            for (;;) {
                try {
                    List<EventObject> chunk = new ArrayList<>();
                    for (EventObject ev = eventQueue.take(); ev != null; ev = eventQueue.poll()) {
                        chunk.add( ev );
                    }
                    new Thread( () -> {
                        LOG.info( "CHUNK: " + chunk.size() );
                        for (EventObject ev : chunk) {
                            fireEvent( ev );
                            synchronized (ev) {
                                ev.notifyAll();
                            }
                        }
                    }).start();
                }
                catch (InterruptedException e) {
                    throw new RuntimeException( e );
                }
            }
        }
    }
}
