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
import areca.common.base.Function.RFunction;
import areca.common.base.Opt;
import areca.common.base.Predicate;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * A {@link Promise} represents the result(s) of an asynchronous computation.
 *
 * @author Falko Bräutigam
 */
public class Promise<T> {

    private static final Log LOG = LogFactory.getLog( Promise.class );

    /**
     * Joins a number of promises into one Promise.
     * <p/>
     * All generated promises <b>must</b> provide just a <b>single value</b>!
     *
     * @param <R>
     * @param num The total number of promises to create/join.
     * @param supplier
     * @return Newly created instance.
     */
    public static <R> Promise<R> joined( int num, RFunction<Integer,Promise<R>> supplier ) {
        return Sequence.ofInts( 0, num-1 )
                .map( i -> supplier.apply( i ) )
                .reduce( (p1, p2) -> p1.join( p2 ) ).get();
    }

    // instance *******************************************

    protected volatile T            waitForResult;

    protected volatile boolean      canceled;

    protected volatile boolean      done;

    protected Throwable             error;

    protected List<BiConsumer<HandlerSite,T,?>> onSuccess = new ArrayList<>();

    protected List<RConsumer<Throwable>> onError = new ArrayList<>();


    public void cancel() {
        canceled = true;
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
        var next = new Completable<R>().upstream( this );
        onSuccess( (self,result) -> {
            var promise = f.apply( result );
            Assert.notNull( promise, "Promise.then(): returned Promise must not be null." );

//            // *inside* the onSuccess handler of the newly created promise (probably) all
//            // upstream elements are already handled; so the first element would trigger
//            // complete (no matter if others are yet to come)
            var isComplete = self.isComplete();

            promise.onSuccess( (_s,_r) -> {
                if (isComplete && _s.isComplete()) {
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
     * @param <R> The type of the value that is send downstream.
     * @param f The function that transforms the value(s).
     * @return A newly created instance of {@link Promise}.
     */
    public <R> Promise<R> map( Function<T,R,Exception> f ) {
        var next = new Completable<R>().upstream( this );
        onSuccess( (self,result) -> {
            if (self.isComplete()) {
                next.complete( f.apply( result ) );
            } else {
                next.consumeResult( f.apply( result ) );
            }
        });
        onError( e -> {
            next.completeWithError( e );
        });
        return next;
    }


    /**
     * Map, filter and/or spread the values.
     * <p>
     * This method does not add any asynchronous computation. It is just a way to add
     * a handler that receives a transformed value.
     *
     * @param <R>
     * @param f The function that transforms the value(s).
     * @return A newly created instance of {@link Promise}.
     */
    public <R> Promise<R> map( BiConsumer<T,Completable<R>,Exception> f ) {
        var next = new Completable<R>().upstream( this );
        onSuccess( (self,result) -> {
            f.accept( result, next );
        });
        onError( e -> {
            next.completeWithError( e );
        });
        return next;
    }


    public Promise<T> filter( Predicate<T,Exception> p ) {
        var next = new Completable<T>().upstream( this );
        onSuccess( (self,result) -> {
            if (p.test( result )) {
                if (self.isComplete()) {
                    next.complete( result );
                } else {
                    next.consumeResult( result );
                }
            }
        });
        onError( e -> {
            next.completeWithError( e );
        });
        return next;
    }


    /**
     * Joins <em>this</em> and <em>other</em> into one Promise.
     * <p/>
     * Both promises <b>must</b> provide just a <b>single value</b>!
     *
     * @param other
     * @return A newly created instance.
     */
    public Promise<T> join( Promise<T> other ) {
        var next = new Completable<T>().upstream( this ).upstream( other );
        MutableInt c = new MutableInt( 0 );
        BiConsumer<HandlerSite,T,Exception> handler = (self,result) -> {
            Assert.that( c.getValue() <= 2 );
            LOG.debug( "JOIN: c = %s, self.complete = %s", c.getValue(), self.isComplete() );
            if (!self.isComplete() || c.incrementAndGet() < 2) {
                LOG.debug( "JOIN: consume: %s", result );
                next.consumeResult( result );
            } else {
                LOG.debug( "JOIN: complete: %s", result );
                next.complete( result );
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


    public <R> Promise<R> reduce( R start, BiConsumer<R,T,Exception> combiner ) {
        var next = new Completable<R>().upstream( this );
        var accu = start;
        onSuccess( (self,result) -> {
            combiner.accept( accu, result );
            if (self.isComplete()) {
                next.complete( accu );
            }
        });
        onError( e -> {
            next.completeWithError( e );
        });
        return next;
    }


    /**
     * Adds the given consumer to the list of consumers that are invoked when the
     * last (TODO ?) value was successfully received.
     * <p/>
     * There is no guarantee about the order in which the consumers are invoked.
     *
     * @param <E>
     * @param consumer Consumes the received value.
     * @return this
     */
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
        return Opt.of( waitForResult );
    }


    /**
     * Interface for success consumers.
     */
    public interface HandlerSite {
        public void cancel();
        public boolean isCanceled();
        public boolean isComplete();
        public int index();
    }


    /**
     * Interface for providers.
     */
    public static class Completable<T>
            extends Promise<T> {

        private boolean complete;

        private int index;

        private HandlerSite site = new HandlerSite() {
            @Override public void cancel() { Completable.this.cancel(); }
            @Override public boolean isCanceled() { return Completable.this.isCanceled(); }
            @Override public boolean isComplete() { return complete; }
            @Override public int index() { return index; }
        };

        private List<Promise<?>> upstreams = new ArrayList<>();


        /**
         * Sets the upstream (parent) instance for this promise.
         */
        protected Completable<T> upstream( Promise<?> upstream ) {
            upstreams.add( upstream );
            return this;
        }


        @Override
        public void cancel() {
            upstreams.forEach( upstream -> upstream.cancel() );
            super.cancel();
        }


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
                LOG.warn( "CONSUME after COMPLETE: " + value.getClass().getName() );
                throw new IllegalStateException();
            }
            // not done, not cancelled -> normal
            else {
                waitForResult = value;
                for (var consumer : onSuccess) {
                    try {
                        consumer.accept( site, value );
                    }
                    catch (Throwable e) {
                        completeWithError( e );
                        break;
                    }
                }
                ++index;
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
                throw (RuntimeException)Platform.rootCause( e );
            }
            // done without error -> programming error
            else if (isDone()) {
                LOG.warn( "ERROR after COMPLETE:" + e );
                throw (RuntimeException)Platform.rootCause( e );
            }
            // not done, not cancelled -> normal
            else {
                try {
                    complete = true;
                    error = e;
                    if (onError.isEmpty()) {
                        LOG.warn( "No onError handler for: " + e, e );
                        // XXX on teavm this helps to see a proper stacktrace
                        throw (RuntimeException)Platform.rootCause( e );
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
                    upstreams = null;
                    done = true;
                    notifyAll();
                }
            }
        }
    }
}
