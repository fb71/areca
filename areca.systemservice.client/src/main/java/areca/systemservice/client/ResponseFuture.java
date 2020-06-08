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
package areca.systemservice.client;

/**
 * Represents the result of an asynchronous {@link SystemServiceClient} method.
 *
 * @param <T>
 * @param <E>
 */
public class ResponseFuture<T, E extends Exception> {

    protected volatile T        value;

    protected volatile boolean  cancelled;

    protected volatile boolean  done;

    protected Exception         exception;

    public boolean cancel( boolean mayInterruptIfRunning ) {
        cancelled = true;
        return true;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public boolean isDone() {
        return done;
    }

    public T waitAndGet() throws E {
        if (!done) {
            synchronized (this) {
                while (!done) {
                    try {
                        wait();
                    }
                    catch (InterruptedException e) {
                    }
                }
            }
        }
        return value;
    }

    protected void setException( Exception e ) {
        this.exception = e;
        setValue( null );  // force notify()
    }

    protected void setValue( T newValue ) {
        synchronized (this) {
            this.done = true;
            this.value = newValue;
            notifyAll();
        }
    }
}