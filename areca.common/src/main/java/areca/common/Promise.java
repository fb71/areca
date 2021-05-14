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

import org.apache.commons.lang3.mutable.MutableInt;

import areca.common.base.BiConsumer;
import areca.common.base.Consumer;
import areca.common.base.Consumer.RConsumer;
import areca.common.base.Function;
import areca.common.base.Opt;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * A {@link Promise} represents the result(s) of an asynchronous computation.
 *
 * @author Falko Bräutigam
 */
public class Promise<T> {

    private static final Log LOG = LogFactory.getLog( Promise.class );

    protected volatile T            result;

    protected volatile boolean      canceled;

    protected volatile boolean      done;

    protected Throwable             error;

    protected List<BiConsumer<HandlerSite,T,?>> onSuccess = new ArrayList<>();

    protected List<RConsumer<Throwable>> onError = new ArrayList<>();


    public boolean cancel() {
        canceled = true;
        return true;
    }


    public boolean isCanceled() {
        return canceled;
    }


    public boolean isDone() {
        return done;
    }


    /**
     * Chain another asynchronous operation. The resulting {@link Promise} receives
     * its value when all predecessors have successfully completed their
     * (asynchronous) work. Errors are just passed through.
     * <p>
     * This method is useful if an asynchronous computation creates another
     * asynchronous computation and so on. Instead of coding inner classes in inner
     * classes this method allows to have a chain of operations. And just one
     * error handling.
     *
     * @param <R> The type of the result value.
     * @param f A function that is called {@link #onSuccess(Consumer)}, does some
     *        computation that results in another Promise.
     * @return A newly created {@link Promise}.
     */
    public <R> Promise<R> then( Function<T,Promise<R>,Exception> f ) {
        var next = new Completable<R>();
        onSuccess( _result -> {
            var promise = f.apply( _result );
            Assert.notNull( promise, "Promise.then(): returned Promise must not be null." );
            promise.onSuccess( (_promise,_r) -> {
                if (_promise.isComplete()) {
                    next.complete( _r );
                } else {
                    next.consumeResult( _r );
                }
            });
            promise.onError( e -> {
                next.completeWithError( e );
            });
        });
        onError( e -> {
            next.completeWithError( e );
        });
        return next;
    }


    /**
     * Creates a new {@link Promise} that receives a transformed/mapped value. The
     * value is transformed via the given {@link Function}. The function is called
     * {@link #onSuccess(Consumer)}. Errors are just passed through.
     * <p>
     * This method does not add any asynchronous computation. It is just a way to add
     * a handler that receives a transformed value.
     *
     * @param <R>
     * @param f The function that transforms the value(s).
     * @return A newly created instance of {@link Promise}.
     */
    public <R> Promise<R> map( Function<T,R,Exception> f ) {
        var next = new Completable<R>();
        onSuccess( (_promise,_r) -> {
            if (_promise.isComplete()) {
                next.complete( f.apply( _r ) );
            } else {
                next.consumeResult( f.apply( _r ) );
            }
        });
        onError( e -> {
            next.completeWithError( e );
        });
        return next;
    }


    public Promise<T> join( Promise<T> other ) {
        var next = new Completable<T>();
        MutableInt c = new MutableInt();
        BiConsumer<HandlerSite,T,Exception> handler = (_promise,_result) -> {
            if (!_promise.isComplete() || c.incrementAndGet() < 2) {
                next.consumeResult( _result );
            } else {
                next.complete( _result );
            }
        };
        onSuccess( handler );
        other.onSuccess( handler );

        RConsumer<Throwable> errorHandler = e -> {
            next.completeWithError( e );
        };
        onError( errorHandler );
        other.onError( errorHandler );
        return next;
    }


    public <E extends Exception> Promise<T> onSuccess( Consumer<T,E> consumer ) {
        return onSuccess( (promise,value) -> consumer.accept( value ) );
    }


    public <E extends Exception> Promise<T> onSuccess( BiConsumer<HandlerSite,T,E> consumer ) {
        onSuccess.add( consumer );
        return this;
    }


    public Promise<T> catchError( RConsumer<Throwable> consumer ) {
        onError( consumer );
        return this;
    }


    public Promise<T> onError( RConsumer<Throwable> consumer ) {
        onError.add( consumer );
        return this;
    }


    /**
     * Just for testing!
     */
    public <E extends Exception> void waitForResult( Consumer<T,E> consumer ) {
        onSuccess( consumer );
        waitForResult();
    }


    /**
     * Just for testing!
     */
    public Opt<T> waitForResult() {
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
        return Opt.of( result );
    }


    /**
     * Interface for success consumers.
     */
    public interface HandlerSite {
        public void cancel();
        public boolean isCanceled();
        public boolean isComplete();
    }


    /**
     * Interface for providers.
     */
    public static class Completable<T>
            extends Promise<T> {

        private boolean complete;

        private HandlerSite site = new HandlerSite() {
            @Override public void cancel() { Completable.this.cancel(); }
            @Override public boolean isCanceled() { return Completable.this.isCanceled(); }
            @Override public boolean isComplete() { return complete; }
        };


        public void complete( T value ) {
            //LOG.debug( "complete(): %s", value );
            complete = true;
            consumeResult( value );
            notifyComplete();
        }


        public void consumeResult( T value ) {
            //LOG.debug( "consumeResult(): %s", value );
            // cancelled -> do nothing
            if (isCanceled()) {
                return;
            }
            // already done with error
            else if (error != null) {
                LOG.warn( "SKIPPING result after error: " + value );
                return;
            }
            // done without error -> programming error
            else if (isDone()) {
                Assert.that( !isDone(), "" );
            }
            // not done, not cancelled -> normal
            else {
                this.result = value;
                for (var consumer : onSuccess) {
                    try {
                        consumer.accept( site, value );
                    }
                    catch (Throwable e) {
                        completeWithError( e );
                        break;
                    }
                }
            }
        }


        public void completeWithError( Throwable e ) {
            //LOG.debug( "completeWithError(): %s", e );
            // cancelled -> do nothing
            if (isCanceled()) {
                return;
            }
            // already done with error
            else if (error != null) {
                LOG.warn( "SKIPPING error after error: " + e );
                return;
            }
            // done without error -> programming error
            else if (isDone()) {
                Assert.that( !isDone(), "" );
            }
            // not done, not cancelled -> normal
            else {
                try {
                    complete = true;
                    error = e;
                    if (onError.isEmpty()) {
                        LOG.warn( "No onError handler for: " + e, e );
                        // XXX on teavm this helps to see a proper stacktrace
                        throw (RuntimeException)Platform.instance().rootCause( e );
                    }
                    else {
                        for (var consumer : onError) {
                            consumer.accept( e );
                        }
                    }
                }
                finally {
                    notifyComplete();
                }
            }
        }


        protected void notifyComplete() {
            //LOG.debug( "complete()" );
            if (!done) {
                synchronized (this) {
                    onSuccess = null; // help GC(?)
                    onError = null;
                    done = true;
                    notifyAll();
                }
            }
        }
    }
}
