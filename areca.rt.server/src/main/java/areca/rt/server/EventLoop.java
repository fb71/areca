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

    /**
     * The standard time to {@link #pendingWait()} is there are background tasks
     * that have {@link #requestPolling() requested polling}.
     */
    public static final int POLLING_TIMEOUT = 402; // unique number

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

    /** The number of currently active request for polling. */
    protected AtomicInteger pollingRequests = new AtomicInteger();


    public void requestPolling() {
        pollingRequests.incrementAndGet();
    }


    public void releasePolling() {
        var c = pollingRequests.decrementAndGet();
        if (c < 0) {
            LOG.warn( "pollingRequests !>= 0 : %s", pollingRequests );
        }
    }


    public void releasePolling( String label, Runnable task, long delayMillis ) {
        enqueue( label, task, delayMillis );
        releasePolling();
    }


    public abstract void enqueue( String label, Runnable task, long delayMillis );


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
        var checkInterval = 100;
//        int timeframeMillis = condition == FULLY
//                ? Integer.MAX_VALUE  // execute all immediate tasks if no condition
//                : (int)POLLING_TIMEOUT;  // execute just 1 (or minimum) tasks between condition checks

        if (!condition.get()) {
            execute( checkInterval );

            var c = 0;
            while (!condition.get()) {
                if (pendingWait() == -1) {
                    return false;
                }
                waitForNewTasks( checkInterval );
                execute( checkInterval );
                c++;
            }
            if (c > 0) {
                LOG.debug( "execute(): poll loop count = %s", c );
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
    public abstract void execute( long timeframeMillis );


    /**
     * The time (in millis) the caller should wait before the queue has more tasks to
     * {@link #execute()}.
     *
     * @return
     *         <ul>
     *         <li><b>0</b> - there are executable tasks</li>
     *         <li>{@link #POLLING_TIMEOUT} - there are background tasks</li>
     *         <li><b>>0</b> - there are delayed tasks</li>
     *         <li><b>-1</b> - queue is completely empty, no tasks pending.</li>
     *         </ul>
     */
    public abstract long pendingWait();


    protected long now() {
        return System.nanoTime() / 1000000;
    }


    /**
     * Inform {@link #waitForNewTasks(long)} that the next run of {@link #execute(int)}
     * probably will find more work to do in the queues.
     */
    protected synchronized void notifyWaitingPollers() {
        notifyAll();
    }


    /**
     * Waits for new tasks to become available for {@link #execute(int)}.
     */
    public synchronized void waitForNewTasks( int millis ) {
        if (pendingWait() <= 0) {
            return;
        }
        var t = Timer.start();
        try {
            wait( millis );
        }
        catch (InterruptedException e) {
            LOG.warn( "Interrupted." );
        }
        LOG.debug( "waitForNewTasks(): %s ms (actual: %s)", millis, t );
    }

}
