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

import static java.lang.Math.max;

import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import java.time.Duration;

import areca.common.Assert;
import areca.common.Session;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * Implements the JS/Browser event loop on the server side.
 * <p/>
 * There is one {@link EventLoop} per {@link Session}. Implementation is not
 * thread-safe as this implemantation expects that everything runs in one single
 * thread.
 * <p/>
 * This implementation handles scheduled/pending tasks, which maybe better belongs to
 * {@link ServerPlatform#schedule(int, java.util.concurrent.Callable)} logic
 * (comparable to {@link #requestPolling()} logic of
 * {@link ServerPlatform#xhr(String, String)}.
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

    private Queue<Task>         queue = new ConcurrentLinkedDeque<>();

    private volatile int        pollingRequests;


    public void requestPolling() {
        pollingRequests ++;
    }

    public void releasePolling( String label, Runnable task, int delayMillis ) {
        enqueue( label, task, delayMillis );
        pollingRequests --;
        Assert.that( pollingRequests >= 0 );
    }

    public void enqueue( String label, Runnable task, int delayMillis ) {
        Assert.that( delayMillis >= 0 );
        LOG.debug( "enqueue(): %s - %s ms", label, delayMillis );
        queue.add( new Task( task, now() + delayMillis, label ) );
    }


    /**
     * Executes pending tasks until queue is empty, no matter how long this may
     * take.
     */
    public void execute() {
        execute( -1 );
    }

    /**
     * Executes pending tasks until queue is empty or the given timeframe exceeds.
     *
     * @param timeframeMillis
     */
    public void execute( int timeframeMillis ) {
        var deadline = timeframeMillis == -1 ? Long.MAX_VALUE : now() + timeframeMillis;

        // one loop can add more tasks to the queue
        var hasMoreWork = true;
        for (var c = 0; hasMoreWork && now() < deadline; c++) {
            LOG.debug( "______ Run %s (queue: %s) ______", c, queue.size() );
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
            hasMoreWork = !canRun.isEmpty();
        }
    }

    /**
     *
     */
    public long pendingWait() {
        //queue.forEach( t -> LOG.debug( "pendingWait(): %s [%s]", t.scheduled - now(), t.label ) );

        // task with min schedule time
        var task = Sequence.of( queue ).reduce( (t1,t2) -> t1.scheduled < t2.scheduled ? t1 : t2  );
        // the pending wait time
        var result = task.map( t -> max( 0, t.scheduled - now() ) ).orElse( -1l );

        //task.ifPresent( t -> LOG.warn( "Min.Task: %s: %s -> %s", t.label, t.scheduled, result ) );

        if (pollingRequests > 0) {
            return result == -1l
                    ? POLLING_TIMEOUT.toMillis()
                    : Math.min( result, POLLING_TIMEOUT.toMillis() );
        }
        else {
            return result;
        }
    }

    protected long now() {
        return System.nanoTime() / 1000000;
    }
}
