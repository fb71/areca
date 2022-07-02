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

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.Callable;

import areca.common.Promise.Completable;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public abstract class Scheduler {

    private static final Log LOG = LogFactory.getLog( Scheduler.class );

    public static Scheduler current() {
        throw new RuntimeException( "not yet implemented" );
    }

    public enum Priority {
        HIGH, STANDARD, DECORATION
    }


    // instance *******************************************

    protected PriorityQueue<Task<?>>   queue = new PriorityQueue<>( new TaskPriority() );


    /**
     * Run the next tick in the loop.
     */
    protected void run( RunInfo info ) {
        var peek = queue.peek();
        while (peek != null && peek.targetTime <= info.now() && info.timeRemaining() > 0) {
            queue.poll();
            // run task... -> promise
            try {
                var result = peek.work.call();
                // ...
            }
            catch (Throwable e) {
                peek.result.completeWithError( e );
            }
            peek = queue.peek();
        }
    }

    /**
     *
     */
    protected interface RunInfo {
        public long now();
        public int timeRemaining();
    }


    protected abstract long now();


    public <R> Promise<R> schedule( Callable<R> task ) {
        return schedule( Priority.STANDARD, 0, task );
    }


    public <R> Promise<R> schedule( Priority prio, int delayMillis, Callable<R> work ) {
        var targetTime = now() + delayMillis;
        var task = new Task<>( prio, targetTime, work );
        queue.add( task );
        return task.result;
    }


    public <R> Promise<R> schedule( Priority prio, Promise<R> task ) {
        throw new RuntimeException( "not yet implemented" );
    }


    /**
     *
     */
    protected class TaskPriority
            implements Comparator<Task<?>> {

        @Override
        public int compare( Task<?> o1, Task<?> o2 ) {
            // XXX Auto-generated method stub
            throw new RuntimeException( "not yet implemented." );
        }
    }


    /**
     *
     */
    protected class Task<T> {

        public Callable<T>              work;

        public Priority                 priority;

        public long                     targetTime;

        public Promise.Completable<T>   result = new Completable<T>();

        public Task( Priority priority, long targetTime, Callable<T> work ) {
            this.priority = priority;
            this.targetTime = targetTime;
            this.work = work;
        }
    }
}
