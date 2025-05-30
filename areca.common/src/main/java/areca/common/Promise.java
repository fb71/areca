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
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableObject;

import areca.common.Scheduler.Priority;
import areca.common.base.BiConsumer;
import areca.common.base.BiFunction;
import areca.common.base.Consumer;
import areca.common.base.Consumer.RConsumer;
import areca.common.base.Function;
import areca.common.base.Function.RFunction;
import areca.common.base.Opt;
import areca.common.base.Predicate;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Level;
import areca.common.log.LogFactory.Log;

/**
 * A {@link Promise} represents the result(s) of an asynchronous computation.
 *
 * @author Falko Bräutigam
 */
public abstract class Promise<T> {

    private static final Log LOG = LogFactory.getLog( Promise.class );

    public static RConsumer<Throwable> defaultErrorHandler = e -> {
        LOG.warn( "No onError handler for: " + e, e );
        // XXX on teavm this helps to see a proper stacktrace
        throw (RuntimeException)Platform.rootCause( e );
    };


    public static void setDefaultErrorHandler( RConsumer<Throwable> defaultErrorHandler ) {
        Promise.defaultErrorHandler = defaultErrorHandler;
    }


    /**
     * Joins a number of promises into one Promise.
     * <p/>
     * All generated promises <b>must</b> provide just a <b>single value</b>!
     *
     * @see #serial(int, RFunction)
     * @param num The total number of promises to create/join.
     * @param supplier
     * @return Newly created instance.
     */
    public static <R> Promise<R> joined( int num, RFunction<Integer,Promise<R>> supplier ) {
        Assert.that( num >= 1 );
        return Sequence.ofInts( 0, num-1 )
                .map( i -> supplier.apply( i ) )
                .reduce( (p1, p2) -> p1.join( p2 ) ).get();
    }


    public static <R> Promise<R> joined( int num, R emptyValue, RFunction<Integer,Promise<R>> supplier ) {
        Assert.that( num >= 0 );
        return num == 0
                ? Promise.completed( emptyValue, null ) // XXX scheduler priority
                : joined( num, supplier );
    }


    /**
     * Joins a number of Promises.
     * <p/>
     * In contrast to {@link #joined(int, RFunction)} the Promises are created one
     * after the other. So there is just one Promise running at a given time.
     *
     * @see #joined(int, RFunction)
     * @param num The total number of promises to create/join.
     * @param supplier The factory that creates the to-be-joined Promises.
     * @return Newly created {@link Promise}.
     */
    public static <R> Promise<R> serial( int num, RFunction<Integer,Promise<R>> supplier ) {
        return new JoinedSerial<>( num, supplier );
    }


    public static <R> Promise<R> serial( int num, R emptyValue, RFunction<Integer,Promise<R>> supplier ) {
        Assert.that( num >= 0 );
        return num == 0
                ? Promise.completed( emptyValue, null ) // XXX scheduler priority
                : serial( num, supplier );
    }

    public static <S,R> Promise<R> serial( Collection<S> elms, R emptyValue, RFunction<S,Promise<R>> supplier ) {
        var it = elms.iterator();
        return serial( elms.size(), emptyValue, i -> {
            Assert.that( it.hasNext() );
            return supplier.apply( it.next() );
        });
    }

    /**
     *
     */
    static class JoinedSerial<R> extends Promise.Completable<R> {
        private int num;
        private int completeCount;
        private RFunction<Integer,Promise<R>> supplier;

        protected JoinedSerial( int num, RFunction<Integer,Promise<R>> supplier ) {
            Assert.that( num > 0 );
            this.num = num;
            this.supplier = supplier;

            upstream( supplier.apply( completeCount )
                    .onSuccess( onSuccessHandler() )
                    .onError( e -> completeWithError( e ) ) );
        }

        protected BiConsumer<HandlerSite,R,Exception> onSuccessHandler() {
            return (self,result) -> {
                LOG.debug( "JOIN: num = %d, c = %d, supplied.complete = %s", num, completeCount, self.isComplete() );
                if (!self.isComplete()) {
                    LOG.debug( "JOIN: consume: %s", result );
                    consumeResult( result );
                }
                else {
                    completeCount ++;
                    if (completeCount < num) {
                        LOG.debug( "JOIN: consume (complete): %s", result );
                        consumeResult( result );
                        upstream( supplier.apply( completeCount )
                                .onSuccess( onSuccessHandler() )
                                .onError( e -> completeWithError( e ) ) );
                    }
                    else {
                        LOG.debug( "JOIN: complete: %s", result );
                        complete( result );
                    }
                }
            };
        }
    }

    // instance *******************************************

    protected volatile T            waitForResult;

    protected volatile Throwable    error;

    protected List<BiConsumer<HandlerSite,T,?>> onSuccess = new ArrayList<>();

    protected List<RConsumer<Throwable>> onError = new ArrayList<>();


    /**
     * Depending on current state:
     * <ul>
     * <li>Not yet completed: complete this Promise with {@link CancelledException}.
     * Prevent any subsequent results to be consumed.</li>
     * <li>Already cancelled: do nothing</li>
     * <li>Already completed: do nothing</li>
     * </ul>
     */
    public abstract void cancel();


    /**
     * True if the Promise was {@link #cancel()}ed by client code.
     */
    public boolean isCanceled() {
        return error instanceof CancelledException;
    }


    /**
     * True if this Promise has received (all) results, or if an error occured, or if
     * it {@link #isCanceled()}.
     */
    public abstract boolean isCompleted();


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
        var promised = new MutableInt( 0 );
        var ready = new MutableInt( 0 );

        onSuccess( (self,result) -> {
            var promise = f.apply( result );
            promised.increment();
            Assert.notNull( promise, "Promise.then(): returned Promise must not be null." );

//            // *inside* the onSuccess handler of the newly created promise (probably) all
//            // upstream elements are already handled; so the first element would trigger
//            // complete (no matter if others are yet to come)
            var isComplete = self.isComplete();

            promise.onSuccess( (_s,_r) -> {
                if (_s.isComplete()) {
                    ready.increment();
                }
                LOG.debug( "then(): promised=%d, ready=%d, self.isComplete=%s (%s)",
                        promised.getValue(), ready.getValue(), self.isComplete(), isComplete );

                if (self.isComplete() && ready.equals( promised )) {
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
     * Chain another async operation and take care of absent values. The given
     * computation is called *just* if the value is present. Otherwise
     * {@link Opt#absent()} is passed through.
     *
     * @param <R>
     * @param f
     * @return A newly created {@link Promise}.
     */
    @SuppressWarnings("unchecked")
    public <R> Promise<Opt<R>> thenOpt( RFunction<T,Promise<R>> f ) {
        return then( (T result) -> {
            Assert.that( result instanceof Opt, "Value must be of type Opt<T>: " + result );
            return ((Opt<T>)result)
                    .ifPresentMap( v -> f.apply( result ).map( r -> Opt.of( r ) ) )
                    .orElse( absent( null ) ); // XXX scheduler priority
            //return opt( (Opt<T>)result, f );
        });
    }


    /**
     * Returnes a new {@link Promise} that is executed by the {@link Scheduler}
     * (rather than in the main event loop).
     * <p>
     * This is useful after {@link Platform#xhr(String, String)} or a DB operation to
     * transfer execution from main JS event loop to the nicer idle scheduler.
     *
     * @param prio The priority of the execution. Null signals that the function is
     *        disabled and just 'this' is returned.
     * @return A newly created {@link Promise}.
     */
    public Promise<T> priority( Priority prio ) {
        return prio == null || prio == Priority.MAIN_EVENT_LOOP
                ? this
                : then( (T result) -> Platform.scheduler.schedule( prio, () -> result ) );
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


    /**
     * Must not filter out the last element that completes the {@link Promise}!
     */
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
     * Experimental, not yet tested.
     */
    @SuppressWarnings( "unchecked" )
    public <R,O> Promise<R> join( Promise<O> other, Class<R> type ) {
        var next = new Completable<R>().upstream( this ).upstream( other );
        MutableInt completeCount = new MutableInt( 0 );
        BiConsumer<HandlerSite,R,Exception> onSuccessHandler = (self,result) -> {
            Assert.that( completeCount.getValue() <= 2 );
            LOG.debug( "JOIN: c = %s, self.complete = %s", completeCount.getValue(), self.isComplete() );
            if (self.isComplete()) {
                 completeCount.increment();
            }
            if (completeCount.getValue() < 2) {
                LOG.debug( "JOIN: consume: %s", result );
                next.consumeResult( result );
            } else {
                LOG.debug( "JOIN: complete: %s", result );
                next.complete( result );
            }
        };
        onSuccess( (BiConsumer<HandlerSite,T,Exception>)onSuccessHandler );
        other.onSuccess( (BiConsumer<HandlerSite,O,Exception>)onSuccessHandler );

        RConsumer<Throwable> errorHandler = e -> {
            next.completeWithError( e );
        };
        onError( errorHandler );
        other.onError( errorHandler );
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
        MutableInt completeCount = new MutableInt( 0 );
        BiConsumer<HandlerSite,T,Exception> onSuccessHandler = (self,result) -> {
            Assert.that( completeCount.getValue() <= 2 );
            LOG.debug( "JOIN: c = %s, self.complete = %s", completeCount.getValue(), self.isComplete() );
            if (self.isComplete()) {
                 completeCount.increment();
            }
            if (completeCount.getValue() < 2) {
                LOG.debug( "JOIN: consume: %s", result );
                next.consumeResult( result );
            } else {
                LOG.debug( "JOIN: complete: %s", result );
                next.complete( result );
            }

//            if (!self.isComplete() || c.incrementAndGet() < 2) {
//                LOG.debug( "JOIN: consume: %s", result );
//                next.consumeResult( result );
//            } else {
//                LOG.debug( "JOIN: complete: %s", result );
//                next.complete( result );
//            }
        };
        onSuccess( onSuccessHandler );
        other.onSuccess( onSuccessHandler );

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


    public <R> Promise<R> reduce2( R start, BiFunction<R,T,R,Exception> combiner ) {
        var next = new Completable<R>().upstream( this );
        var accu = new MutableObject<>( start  );
        onSuccess( (self,result) -> {
            accu.setValue( combiner.apply( accu.getValue(), result ) );
            if (self.isComplete()) {
                next.complete( accu.getValue() );
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


//    /**
//     * Schedule this Promise with the given priority. This gives the
//     * {@link Scheduler} control over the execution. Success handlers that are
//     * registered with the resulting {@link Promise} are called just when the
//     * Scheduler decides to do so based on time and priority.
//     *
//     * @param priority
//     * @return Newly created scheduled {@link Promise}.
//     */
//    public Promise<T> schedule( Scheduler.Priority priority ) {
//        return Scheduler.current().schedule( priority, this );
//    }


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
        Platform.waitForCondition( () -> isCompleted(), Promise.this );
        return Opt.of( waitForResult );
    }


    public Opt<T> opt() {
        return isCompleted() ? Opt.of( waitForResult ) : Opt.absent();
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
     * Signals that the {@link Promise} was cancelled.
     */
    public static class CancelledException
            extends RuntimeException {

        protected CancelledException() {
            super( "Operation was cancelled." );
        }

        protected CancelledException( String message ) {
            super( message );
        }

        protected CancelledException( String message, Throwable cause ) {
            super( message, cause );
        }
    }


    /**
     * Interface for providers.
     */
    public static class Completable<T>
            extends Promise<T> {

        private HandlerSite site = new HandlerSite() {
            @Override public void cancel() { Completable.this.cancel(); }
            @Override public boolean isCanceled() { return Completable.this.isCanceled(); }
            @Override public boolean isComplete() { return aboutToComplete; }
            @Override public int index() { return index; }
        };

        protected enum State {
            INITIALIZED, CONSUMING, COMPLETED, CANCELED;
        }

        private volatile State  state = State.INITIALIZED;

        private boolean         aboutToComplete;

        private int             index;

        private List<Promise<?>> upstreams = new ArrayList<>();


        @Override
        public <E extends Exception> Promise<T> onSuccess( BiConsumer<HandlerSite,T,E> consumer ) {
            if (isCompleted()) {
                //LOG.warn( "onSuccess(): already completed! (value=%s)", waitForResult );
                if (error == null) {
                    doConsume( consumer, waitForResult );
                }
                // avoid super so that onSuccess() can be called many times for an
                // already completed Promise
                return this;
            }
            return super.onSuccess( consumer );
        }


        @Override
        public Promise<T> onError( RConsumer<Throwable> consumer ) {
            if (isCompleted()) {
                //LOG.warn( "onSuccess(): already completed! (value=%s)", waitForResult );
                if (error != null) {
                    consumer.accept( error );
                }
                return this;
            }
            return super.onError( consumer );
        }


        /**
         * Sets the upstream (parent) instance for this promise.
         */
        protected Completable<T> upstream( Promise<?> upstream ) {
            upstreams.add( upstream );
            return this;
        }


        @Override
        public boolean isCompleted() {
            return state.ordinal() >= State.COMPLETED.ordinal();
        }


        /**
         *
         */
        protected void raiseState( State newState, Runnable ifSuccessful ) {
            //LOG.info( "[%s] : %s -> %s", StringUtils.right( ""+hashCode(), 3 ), state, newState );

            // CANCELLED or ERROR
            if (state == State.CANCELED || error != null) {
                // do nothing
            }
            // -> CANCELLED
            else if (newState == State.CANCELED) {
                if (state == State.COMPLETED) {
                    // do nothing
                }
                else {
                    setState( newState, ifSuccessful );
                }
            }
            // downgrade not allowed
            else if (newState.ordinal() < state.ordinal()) {
                throw new IllegalStateException( "Wrong state raise: " + state + " -> " + newState );
            }
            // -> CONSUMING
            else if (newState == State.CONSUMING) {
                setState( newState, ifSuccessful );
            }
            // -> COMPLETED
            else if (newState == State.COMPLETED) {
                // error in onSuccess() handler results in: COMPLETED -> COMPLETED; ok
                // -> no check if already COMPLETED
                aboutToComplete = true;
                setState( newState, ifSuccessful );
            }
        }


        protected void setState( State newState, Runnable task ) {
            //LOG.info( "[%s] %s\t-> %s", StringUtils.right( ""+hashCode(), 3 ), state, newState );
            state = newState;
            task.run();
        }


        @Override
        public void cancel() {
            completeWithError( new CancelledException() );
            raiseState( State.CANCELED, () -> {
                upstreams.forEach( upstream -> upstream.cancel() );
            });
        }


        public void complete( T value ) {
            raiseState( State.COMPLETED, () -> {
                waitForResult = value;
                doConsume( value );
                notifyComplete();
            });
        }


        public void consumeResult( T value ) {
            raiseState( State.CONSUMING, () -> {
                doConsume( value );
                ++index;
            });
        }


        protected void doConsume( T value ) {
            for (var consumer : onSuccess) {
                if (doConsume( consumer, value ) != null) {
                    break;
                }
            }
        }


        public Throwable doConsume( BiConsumer<HandlerSite,T,?> consumer, T value ) {
            try {
                consumer.accept( site, value );
                return null;
            }
            catch (Throwable e) {
                // INFO enabled means debug is on
                // XXX if debug=true the defaultErrorHandler should throw the exception for TeaVM
                // XXX but it does not happen :(
                if (!Platform.isJVM() && LOG.isLevelEnabled( Level.INFO )) {
                    throw (RuntimeException)e;
                } else {
                    LOG.warn( "doConsume(): %s", e.toString() );
                }
                completeWithError( e );
                return e;
            }
        }


        public void completeWithError( Throwable e ) {
            raiseState( State.COMPLETED, () -> {
                try {
                    error = e;
                    if (onError.isEmpty()) {
                        LOG.info( "Default error handler: %s", e.toString() );
                        defaultErrorHandler.accept( e );
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
            });
        }


        protected void notifyComplete() {
            synchronized (this) {
                //LOG.info( "notify: %s", this );
                notifyAll();
            }
            upstreams.forEach( upstream -> ((Completable<?>)upstream).notifyComplete() );
        }
    }


    /**
     * See {@link #thenOpt(RFunction)}
     * @see #thenOpt(RFunction)
     */
    public static <R> Promise<Opt<R>> absent( Priority priority ) {
        return completed( Opt.<R>absent(), priority );
    }


//    /**
//     * Propagates a - maybe absent - value.
//     *
//     * @param value
//     * @param f
//     */
//    public static <V,R> Promise<Opt<R>> opt( Opt<V> value, RFunction<V,Promise<R>> f ) {
//        return value
//                .ifPresentMap( v -> f.apply( v ).map( r -> Opt.of( r ) ) )
//                .orElse( completed( Opt.<R>absent() ) );
//    }


    /**
     * Returns a {@link Promise} that (immediatelly) delivers the given value.
     *
     * @param priority The priority the {@link Scheduler} should execute the result
     *        of this operation with, or null to execute in the main JS event loop.
     */
    public static <R> Promise<R> completed( R value, Priority priority ) {
        return priority != null && priority != Priority.MAIN_EVENT_LOOP
                ? Platform.scheduler.schedule( priority, () -> value )
                : Platform.async( () -> value );
    }


    /**
     * Returns a {@link Promise} that delivers the given value via
     * {@link Platform#async(Runnable)}.
     */
    public static <R> Promise<R> async( R value ) {
        return Platform.async( () -> value );
    }

}
