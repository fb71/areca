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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;

import java.time.Duration;

import areca.common.Assert;
import areca.common.Session;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * Mimics the JS/Browser event loop on the server side.
 * <p>
 * There is one {@link EventLoop} per {@link Session}. Implementation is not
 * thread-safe as this implemantation expects that everything runs in one single
 * thread.
 *
 * @author Falko Bräutigam
 */
public class EventLoop {

    private static final Log LOG = LogFactory.getLog( EventLoop.class );

    public static final Duration POLLING_TIMEOUT = Duration.ofMillis( 250 );

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

    // instance *******************************************

    private Deque<Task>         queue = new ArrayDeque<>( 128 );

    private volatile int        pollingRequests;


    public void requestPolling() {
        pollingRequests ++;
    }

    public void releasePolling() {
        pollingRequests --;
        Assert.that( pollingRequests >= 0 );
    }

    public void enqueue( String label, Runnable task, int delayMillis ) {
        LOG.debug( "enqueue(): %s - %s ms", label, delayMillis );
        queue.addLast( new Task( task, now() + delayMillis, label ) );
    }

    /**
     * Executes pending tasks until queue is empty.
     */
    public void execute( int timeframeMillis ) {
        // one loop can add more tasks to the queue
        var c = 0;
        for (var moreWork = true; moreWork; c++) {
            LOG.info( "______ Run %s (queue: %s) ______", c, queue.size() );
            var _now = now();
            var canRun = new ArrayList<Task>( queue.size() );
            for (var it = queue.iterator(); it.hasNext();) {
                var task = it.next();
                if (task.scheduled <= _now) {
                    it.remove();
                    canRun.add( task );
                }
            }
            for (Task task : canRun) {
                task.task.run();
            }
            moreWork = !canRun.isEmpty();
        }
    }

    public long pendingWait() {
        Sequence.of( queue ).forEach( t -> LOG.info( "pendingWait(): %s: %s", t.label, t.scheduled - now() ) );
        var result = Sequence.of( queue )
                .map( t -> t.scheduled ).reduce( Math::min )
                .map( minScheduled -> Math.max( 0, minScheduled - now() ) )
                .orElse( -1l );

        if (pollingRequests > 0) {
            return result == -1l ? POLLING_TIMEOUT.toMillis() : Math.min( result, POLLING_TIMEOUT.toMillis() );
        }
        else {
            return result;
        }
    }

    protected long now() {
        return System.nanoTime() / 1000000;
    }
}
