/*
 * Copyright (C) 2023, the @authors. All rights reserved.
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
package areca.rt.server;

import java.time.Duration;

import areca.common.Session;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * Implements the JS/Browser event loop on the server side.
 * <p/>
 * There is one {@link EventLoop} per {@link Session}. Implementation does not
 * need to be thread-safe as we expect that everything runs in one single
 * thread.
 *
 * @author Falko Bräutigam
 */
public abstract class EventLoop {

    private static final Log LOG = LogFactory.getLog( EventLoop.class );

    public static final Duration POLLING_TIMEOUT = Duration.ofMillis( 250 );

    public static EventLoop create() {
        return new EventLoop1();
    }

    protected static class Task {
        public String label;
        public Runnable task;
        public long scheduled;

        public Task( Runnable task, long scheduled, String label ) {
            this.label = label;
            this.task = task;
            this.scheduled = scheduled;
        }
    }

    public abstract void requestPolling();

    public abstract void releasePolling();

    public void releasePolling( String label, Runnable task, int delayMillis ) {
        enqueue( label, task, delayMillis );
        releasePolling();
    }

    public abstract void enqueue( String label, Runnable task, int delayMillis );

    /**
     * Executes pending tasks until queue is empty, no matter how long this may take.
     */
    public void execute() {
        execute( -1 );
    }

    /**
     * Executes pending tasks until queue is empty or the given timeframe exceeds.
     * @param timeframeMillis The maximum time to spent in this method, or -1.
     */
    public abstract void execute( int timeframeMillis );

    public abstract long pendingWait();

    protected long now() {
        return System.nanoTime() / 1000000;
    }

}
