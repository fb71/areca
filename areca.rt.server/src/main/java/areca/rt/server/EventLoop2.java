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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import areca.common.Assert;
import areca.common.Timer;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * ...
 * <p>
 * This implementation handles scheduled/pending tasks, which maybe better belong to
 * {@link ServerPlatform#schedule(int, java.util.concurrent.Callable)} logic
 * (comparable to {@link #requestPolling()} logic of
 * {@link ServerPlatform#xhr(String, String)}.
 *
 * @author Falko Bräutigam
 */
public class EventLoop2
        extends EventLoop {

    private static final Log LOG = LogFactory.getLog( EventLoop2.class );

    private Queue<Task>         queue = new ConcurrentLinkedQueue<>();

    private Queue<Task>         delayed = new ConcurrentLinkedQueue<>();

    private volatile int        pollingRequests;


    public void requestPolling() {
        pollingRequests ++;
    }


    public void releasePolling() {
        pollingRequests --;
        Assert.that( pollingRequests >= 0 );
    }


    public void enqueue( String label, Runnable task, int delayMillis ) {
        Assert.that( delayMillis >= 0 );
        LOG.debug( "enqueue(): %s - %s ms", label, delayMillis );
        (delayMillis == 0 ? queue : delayed)
                .add( new Task( task, now() + delayMillis, label ) );
    }


    /**
     * Executes pending tasks until queue is empty or the given timeframe exceeds.
     *
     * @param timeframeMillis The maximum time to spent in this method, or -1.
     */
    public void execute( int timeframeMillis ) {
        var deadline = timeframeMillis == -1 ? Long.MAX_VALUE : now() + timeframeMillis;

        // delayed task eligable to run
        var _now = now();
        for (var it = delayed.iterator(); it.hasNext();) {
            var task = it.next();
            if (task.scheduled <= _now) {
                it.remove();
                queue.add( task );
                LOG.debug( "    eligable: %s", task.label );
            }
        }

        // loop queue
        LOG.debug( "______ Run (queue: %s) ______", queue.size() );
        var t = Timer.start();
        var count = 0;
        while (!queue.isEmpty() && (now() < deadline || count == 0)) { // at least one
            var task = queue.poll();
            task.task.run();
            count ++;
        }
        if (!queue.isEmpty()) {
            LOG.debug( "Break: queue=%s, count=%s [%s]", queue.size(), count, t );
        }
        LOG.debug( "______ End (queue: %s, count = %s [%s])", queue.size(), count, t );
    }

    /**
     *
     */
    public long pendingWait() {
        //queue.forEach( t -> LOG.debug( "pendingWait(): %s [%s]", t.scheduled - now(), t.label ) );

        if (!queue.isEmpty()) {
            return 0;
        }

        var result = Sequence.of( delayed )
                // task with min schedule time
                .reduce( (t1,t2) -> t1.scheduled < t2.scheduled ? t1 : t2  )
                .map( t -> max( 0, t.scheduled - now() ) )
                .orElse( -1l );

        if (pollingRequests > 0) {
            return result == -1l
                    ? POLLING_TIMEOUT.toMillis()
                    : Math.min( result, POLLING_TIMEOUT.toMillis() );
        }
        else {
            return result;
        }
    }

}
