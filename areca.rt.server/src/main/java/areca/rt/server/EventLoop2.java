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

import static areca.common.log.LogFactory.Level.DEBUG;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import areca.common.Assert;
import areca.common.Timer;
import areca.common.base.Opt;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * Second implementation of {@link EventLoop}, with separate queue for
 * {@link #delayed} tasks.
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

    //private static java.util.Timer timer = new java.util.Timer( true );

    private Queue<Task>     queue = new ConcurrentLinkedQueue<>();

    private DelayedQueue    delayed = new DelayedQueue();


    @Override
    public void enqueue( String label, Runnable task, long delayMillis ) {
        Assert.that( delayMillis >= 0 );
        LOG.debug( "enqueue(): %s - %s ms", label, delayMillis );

        var t = new Task( task, now() + delayMillis, label );
        if (delayMillis == 0) {
            queue.add( t );
            notifyWaitingPollers();
        }
        else {
            delayed.add( t );
//            timer.schedule( new TimerTask() {
//                @Override
//                public void run() {
//                    LOG.warn( "Schedule delayed: '%s' (%s ms)", label, delayMillis );
//                    var removed = delayed.remove( t );
//                    Assert.that( removed );
//                    queue.add( t );
//                }
//            }, delayMillis );
        }
    }


    protected void checkAndEnqueueDelayed() {
        var now = now();
        for (var t = delayed.poll( now ); t != null; t = delayed.poll( now )) {
            //LOG.warn( "Schedule delayed: '%s'", t.label );
            queue.add( t );
            notifyWaitingPollers();
        }
    }


    @Override
    public void execute( long timeframeMillis ) {
        var deadline = timeframeMillis == -1 ? Long.MAX_VALUE : now() + timeframeMillis;

        checkAndEnqueueDelayed();

        // queue loop
        LOG.debug( "______ Run (queue: %s) ______", queue.size() );
        var t = Timer.start();
        var count = 0;
        var task = queue.poll();
        while (task != null) { // at least one
            try {
                task.task.run();
            }
            catch (Throwable e) {
                defaultErrorHandler.accept( e );
            }
            task = now() < deadline ? queue.poll() : null;
            count ++;

            // checkDelayed();
        }
        if (LOG.isLevelEnabled( DEBUG ) && !queue.isEmpty() && timeframeMillis > 0) {
            LOG.debug( "Break: queue=%s, count=%s [%s]", queue.size(), count, t );
        }
        LOG.debug( "______ End (queue: %s, count = %s [%s])", queue.size(), count, t );
    }


    @Override
    public long pendingWait() {
        //queue.forEach( t -> LOG.debug( "pendingWait(): %s [%s]", t.scheduled - now(), t.label ) );

        if (!queue.isEmpty()) {
            return 0;
        }
        else if (pollingRequests.get() > 0) {
            return POLLING_TIMEOUT;
        }
        else if (!delayed.isEmpty()) {
            return Math.max( 0, delayed.minScheduled().get() - now() );
        }
        else {
            return -1l;
        }
    }


    /**
     *
     */
    protected static class DelayedQueue
            extends PriorityQueue<Task> {

        private static final Comparator<Long> NATURAL_ORDER = Comparator.<Long>naturalOrder();

        protected DelayedQueue() {
            super( (t1,t2) -> NATURAL_ORDER.compare( t1.scheduled, t2.scheduled ) );
        }

        public Opt<Long> minScheduled() {
            return head().map( t -> t.scheduled ); //.orElse( -1l );
        }

        public Task poll( long timestamp ) {
            var peek = peek();
            if (peek != null && peek.scheduled <= timestamp) {
                Assert.isSame( peek, poll() );
                return peek;
            }
            return null;
        }

        public Opt<Task> head() {
            return Opt.of(  peek() );
        }
    }


//    /**
//     *
//     */
//    protected static class BackgroundTimer
//            extends java.util.Timer {
//
//        private Object taskQueue;
//
//        private Method getMin;
//
//        protected BackgroundTimer() {
//            super( true );
//            try {
//                taskQueue = FieldUtils.readField( this, "queue", true );
//                getMin = MethodUtils.getMatchingAccessibleMethod( taskQueue.getClass(), "getMin", new Class[0]  );
//            }
//            catch (IllegalAccessException e) {
//                throw new RuntimeException( e );
//            }
//        }
//
//        public TimerTask getMin() {
//            try {
//                return (TimerTask)getMin.invoke( taskQueue );
//            }
//            catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
//                throw new RuntimeException( e );
//            }
//        }
//    }
}
