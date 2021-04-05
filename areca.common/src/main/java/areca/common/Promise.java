/*
 * Copyright (C) 2021, the @authors. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;

import areca.common.base.Consumer;

/**
 *
 * @author Falko Bräutigam
 */
public class Promise<T> {

    protected volatile T            value;

    protected volatile boolean      cancelled;

    protected volatile boolean      done;

    protected Exception             exception;

    protected List<Consumer<T,?>>   onSuccess = new ArrayList<>();

    protected List<Consumer.$<Exception>> onError = new ArrayList<>();


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


    public <E extends Exception> Promise<T> then( Consumer<T,E> consumer ) {
        onSuccess.add( consumer );
        return this;
    }


    public Promise<T> catchError( Consumer.$<Exception> consumer ) {
        onError( consumer );
        return this;
    }


    public Promise<T> onError( Consumer.$<Exception> consumer ) {
        onError.add( consumer );
        return this;
    }


    public <E extends Exception> void thenWait( Consumer<T,E> consumer ) {
        onSuccess.add( consumer );
        thenWait();
    }


    public T thenWait() {
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


    /**
     * API for providers.
     */
    public static class Completable<T>
            extends Promise<T> {

        public void completeWithError( Exception e ) {
            this.exception = e;
            if (onError.isEmpty()) {
                System.err.println( "Exception: " + e );
                throw (RuntimeException)e;
            }
            else {
                for (var consumer : onError) {
                    consumer.accept( e );
                }
            }
            // complete( null );  // force notify()
        }


        public void complete( T newValue ) {
            for (var consumer : onSuccess) {
                try {
                    consumer.accept( newValue );
                }
                catch (Throwable e) {
                    throw new UnsupportedOperationException( "not yet implemented" );
                }
            }
            synchronized (this) {
                this.done = true;
                this.value = newValue;
                notifyAll();
            }
        }
    }
}
