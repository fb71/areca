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

import java.util.concurrent.atomic.AtomicInteger;

import java.time.Duration;

import areca.common.Session;
import areca.common.Timer;
import areca.common.base.Consumer.RConsumer;
import areca.common.base.Supplier.RSupplier;
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

    public static final Duration POLLING_TIMEOUT = Duration.ofMillis( 500 );

    public static final RSupplier<Boolean> FULLY = () -> false;

    protected static RConsumer<Throwable> defaultErrorHandler = e -> {
        LOG.warn( "defaultErrorHandler: %s", e.toString() );
        e.printStackTrace( System.err );
    };


    public static void setDefaultErrorHandler( RConsumer<Throwable> defaultErrorHandler ) {
        EventLoop.defaultErrorHandler = defaultErrorHandler;
    }

    /**
     * Factory
     */
    public static EventLoop create() {
        return new EventLoop2();
    }

    /**
     * Enqueued task.
     */
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

    protected AtomicInteger pollingRequests = new AtomicInteger();


    public void requestPolling() {
        pollingRequests.incrementAndGet();
        //LOG.warn( "POLLING: +%s", pollingRequests );
    }


    public void releasePolling() {
        var c = pollingRequests.decrementAndGet();
        //LOG.warn( "POLLING: -%s", c );
        if (c < 0) {
            LOG.warn( "pollingRequests !>= 0 : %s", pollingRequests );
            //pollingRequests = 0;
        }
        //Assert.that( pollingRequests >= 0, "pollingRequests !>= 0 : " + pollingRequests );
    }


    public void releasePolling( String label, Runnable task, int delayMillis ) {
        enqueue( label, task, delayMillis );
        releasePolling();
    }


    public abstract void enqueue( String label, Runnable task, int delayMillis );


    /**
     * Executes pending tasks until the given condition is met or there
     * are no more tasks to execute.
     *
     * @param condition
     * @param timeframeMillis
     * @return False if the given condition was not met.
     * @see EventLoop#FULLY
     */
    public boolean execute( RSupplier<Boolean> condition ) {
        int timeframeMillis = condition == FULLY
                ? -1  // execute all immediate tasks if no condition
                : 0;  // execute just 1 (or minimum) tasks between condition checks

        if (!condition.get()) {
            execute( timeframeMillis );
            while (!condition.get()) {
                long pendingWait = pendingWait();

                if (pendingWait == -1) {
                    return false;
                }
                if (pendingWait > 0) {
                    // waiting on target (Promise) is tricky because one Promise usually
                    // consists of a chain of promises...
                    synchronized (this) {
                        var t = Timer.start();
                        try { wait( pendingWait ); } catch (InterruptedException e) { }
                        LOG.info( "waited: %s ms (actual: %s)", pendingWait, t );
                    }
                }
                execute( timeframeMillis );
            }
        }
        return true;
    }


    /**
     * Executes pending tasks until queue has no more entries for *immediate*
     * execution or the given timeframe exceeds.
     *
     * @param timeframeMillis The maximum time to spent in this method, or -1.
     * @see #pendingWait()
     */
    public abstract void execute( int timeframeMillis );


    /**
     * The time the caller should wait before the queue has more tasks to {@link #execute()}.
     *
     * @return <ul>
     * <li>0: there are executable tasks</li>
     * <li>>0: wait for the given amount of time</li>
     * <li>-1: queue is completely empty, no tasks pending.</li>
     * </ul>
     */
    public abstract long pendingWait();


    protected long now() {
        return System.nanoTime() / 1000000;
    }

}
