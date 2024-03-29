/*
 * Copyright (C) 2022, the @authors. All rights reserved.
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
package areca.common;

import static areca.common.log.LogFactory.Level.DEBUG;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.concurrent.Callable;

import areca.common.Platform.IdleDeadline;
import areca.common.Promise.Completable;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * Advanced task scheduler based on the {@link Platform}s requestIdleCallback
 * priority queue.
 * <p>
 * In contrast to the main event loop ({@link Platform#async(Callable)}) tasks run
 * only if, and only as long as, the runtime decides that there is idle time. So we
 * don't break browser rendering and animations.
 * <p>
 * Also we (plan to) do some priority handling to add even finer grained control.
 *
 * @author Falko Bräutigam
 */
public class Scheduler {

    private static final Log LOG = LogFactory.getLog( Scheduler.class );

    public enum Priority {
        MAIN_EVENT_LOOP,
        INTERACTIVE, BACKGROUND, DECORATION
    }


    // instance *******************************************

    // XXX protected PriorityQueue<Task<?>>    queue = new PriorityQueue<>( 256, new TaskPriority() );
    protected Deque<Task<?>>            queue = new ArrayDeque<>( 256 );

    protected Promise<Void>             async;


    public <R> Promise<R> schedule( Callable<R> task ) {
        return schedule( Priority.BACKGROUND, 0, task );
    }


    public <R> Promise<R> schedule( Priority prio, Callable<R> task ) {
        return schedule( prio, 0, task );
    }

    public void schedule( Priority prio, Runnable task ) {
        schedule( prio, 0, () -> {
            task.run();
            return null;
        });
    }


    public <R> Promise<R> schedule( Priority prio, int delayMillis, Callable<R> work ) {
        Assert.notNull( prio, "Priority must not be null" );
        Assert.that( prio != Priority.MAIN_EVENT_LOOP, "MAIN_EVENT_LOOP is special priority for Promise" );
        Assert.isEqual( 0, delayMillis, "Delayed schedule is not yet supported." );
        if (async == null) {
            //Assert.isNull( async );
            async = Platform.requestIdleCallback( deadline -> process( deadline ) );
        }

        var task = new Task<>( prio, now() + delayMillis, work );
        queue.addLast( task );
        return task.promise;
    }


    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void process( IdleDeadline deadline ) {
        if (LOG.isLevelEnabled( DEBUG )) { // LOG potentially uses Scheduler
            System.out.println( LOG.format( DEBUG, "Queue: %d, remaining: %s", queue.size(), deadline.timeRemaining() ) );
        }
        //var now = now();

        async = null;

        // handle tasks
        int count = 0;
        Task peek = queue.peek();
        while (peek != null /*&& peek.targetTime <= now*/ && deadline.timeRemaining() > 0) {
            queue.poll();
            try {
                var result = peek.work.call();
                peek.promise.complete( result );
            }
            catch (Throwable e) {
                peek.promise.completeWithError( e );
            }
            peek = queue.peek();
            count ++;
        }
        if (LOG.isLevelEnabled( DEBUG )) {
            System.out.println( LOG.format( DEBUG, "  processed: %d, queue: %d, remaining: %s, async: %s", count, queue.size(), deadline.timeRemaining(), async ) );
        }

        // schedule next loop
        if (!queue.isEmpty() && async == null) {
            async = Platform.requestIdleCallback( dl -> process( dl ) );
        }
    }


    protected long now() {
        return System.nanoTime() / 1000000;
    }


    /**
     *
     */
    protected class TaskPriority
            implements Comparator<Task<?>> {

        @Override
        public int compare( Task<?> t1, Task<?> t2 ) {
            return t1.priority != t2.priority
                    ? t1.priority.ordinal() > t2.priority.ordinal() ? 1 : -1
                    : t1.targetTime > t2.targetTime ? 1 : -1;
        }
    }


    /**
     *
     */
    protected class Task<T> {

        public Callable<T>              work;

        public Priority                 priority;

        public long                     targetTime;

        public Promise.Completable<T>   promise = new Completable<T>();

        public Task( Priority priority, long targetTime, Callable<T> work ) {
            this.priority = priority;
            this.targetTime = targetTime;
            this.work = work;
        }
    }
}
