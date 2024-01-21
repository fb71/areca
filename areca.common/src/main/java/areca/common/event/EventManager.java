/*
 * Copyright (C) 2019-2022, the @authors. All rights reserved.
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
package areca.common.event;

import static areca.common.Assert.notNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import areca.common.Assert;
import areca.common.Promise;
import areca.common.Session;
import areca.common.Timer;
import areca.common.base.Opt;
import areca.common.base.Predicate;
import areca.common.base.Predicate.RPredicate;
import areca.common.base.Supplier;
import areca.common.base.Supplier.RSupplier;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.MethodInfo;

/**
 *
 * @author falko
 */
public abstract class EventManager {

    private static final Log LOG = LogFactory.getLog( EventManager.class );

    /**
     * The instance for the current application session.
     */
    public static EventManager instance() {
        return Session.instanceOf( EventManager.class );
    }

    // instance *******************************************

    public BiConsumer<EventObject,Throwable>    defaultOnError;

    /** Copy-on-Write */
    protected List<EventHandlerInfoImpl>        handlers = Collections.emptyList();

    @SuppressWarnings("rawtypes")
    protected Map<Pair,MethodInfo>              methodCache = new HashMap<>( 128 );


    protected EventManager() {
        defaultOnError = (ev, e) -> {
            LOG.warn( "Error during handling of event: " + ev, e );

            // FIXME no (correct) StackTrace in TeaVM; just throwing shows something useful :(
//            System.out.println( "STACK: " + e.getStackTrace().length );
//            for (StackTraceElement elm : e.getStackTrace()) {
//                System.err.println( elm.getClassName() + "." + elm.getMethodName() + " : " + elm.getLineNumber() );
//            }

            throw (RuntimeException)e;

//            if (e instanceof RuntimeException) {
//                throw (RuntimeException)e;
//            }
//            else if (e instanceof Error) {
//                throw (Error)e;
//            }
//            else {
//                throw new RuntimeException( e );
//            }
        };
    }


    public EventHandlerInfo subscribe( EventListener<?> l ) {
        return subscribe( (Object)l );
    }


    public EventHandlerInfo subscribe( Object annotatedOrListener ) {
        var newHandler = new EventHandlerInfoImpl( annotatedOrListener );

        var newHandlers = new ArrayList<EventHandlerInfoImpl>( handlers.size() + 1 );
        // XXX arraycopy?
        for (var cursor : handlers) {
            if (cursor.handler == annotatedOrListener
                    && (cursor.unsubscribeIf == null || !cursor.unsubscribeIf.get())) {
                throw new IllegalStateException( "Event handler already subscribed! " );
            }
            newHandlers.add( cursor );
        }
        newHandlers.add( newHandler );
        Assert.isEqual( handlers.size()+1, newHandlers.size() );
        handlers = newHandlers;
        return newHandler;
    }


    protected void unsubscribe( Set<? extends EventHandlerInfo> remove ) {
        var t = Timer.start();
        var newHandlers = new ArrayList<EventHandlerInfoImpl>( handlers.size() - remove.size() );
        for (var handler : handlers) {
            if (!remove.contains( handler )) {
                newHandlers.add( handler );
            }
        }
        //Assert.isEqual( handlers.size()-remove.size(), newHandlers.size() );
        if (handlers.size()-remove.size() != newHandlers.size()) {
            LOG.info( "!!!Unsubscribe: expected:%s != newHandlers:%s", handlers.size()-remove.size(), newHandlers.size() );
        }
        LOG.debug( "Expunged: %s, now: %s (%s) (%s)", remove.size(), newHandlers.size(),
                t.elapsedHumanReadable(), getClass().getSimpleName() );
        handlers = newHandlers;
    }


    protected int keyOf( Object annotatedOrListener ) {
        return System.identityHashCode( annotatedOrListener );
    }


    /**
     * Fires the given event. Depending on the actual EventManager implementation
     * the event processing is done asynchronously.
     *
     * @see #publish2(EventObject)
     */
    public abstract void publish( EventObject ev );


    /**
     * Fires the given event. Depending on the actual EventManager implementation the
     * event processing is done asynchronously.
     *
     * @return {@link Promise} can be used to do things after the event was actually
     *         fired and processed by all the current handlers.
     */
    public abstract Promise<Void> publish2( EventObject ev );


    /**
     *
     */
    public interface EventHandlerInfo {

        /**
         * For multiple invocations of this method the predicates are composed
         * by logical AND.
         *
         * @param performIf
         * @return this
         */
        EventHandlerInfo performIf( RPredicate<EventObject> performIf );

        EventHandlerInfo unsubscribeIf( RSupplier<Boolean> unsubscribeIf );

        <E extends EventObject> EventHandlerInfo performIf( Class<E> type, RPredicate<E> performIf );
    }

    /**
     *
     */
    public class EventHandlerInfoImpl
            implements EventHandlerInfo {

        protected Object                    handler;

        public BiConsumer<EventObject,Throwable>         onError = defaultOnError;

        public Predicate<EventObject,RuntimeException>   performIf;

        public Supplier<Boolean,RuntimeException>        unsubscribeIf;


        public EventHandlerInfoImpl( Object handler ) {
            this.handler = notNull( handler );
        }


        @Override
        @SuppressWarnings("hiding")
        public EventHandlerInfo performIf( RPredicate<EventObject> performIf ) {
            Assert.notNull( performIf );
            this.performIf = this.performIf != null ? this.performIf.and( performIf ) : performIf;
            return this;
        }


        @Override
        @SuppressWarnings({"hiding", "unchecked"})
        public <E extends EventObject> EventHandlerInfo performIf( Class<E> type, RPredicate<E> performIf ) {
            Assert.notNull( performIf );
            Assert.isNull( this.performIf );
            this.performIf = (EventObject ev) ->
                    type.isAssignableFrom( ev.getClass() ) &&
                    performIf.test( (E)ev );
            return this;
        }


        @Override
        @SuppressWarnings("hiding")
        public EventHandlerInfo unsubscribeIf( RSupplier<Boolean> unsubscribeIf ) {
            var current = this.unsubscribeIf;
            this.unsubscribeIf = current != null
                    ? () -> current.get() || unsubscribeIf.get()
                    : unsubscribeIf;
            return this;
        }


        public void perform( EventObject ev ) {
            try {
                // filter
                if (performIf != null && !performIf.test( ev )) {
                    return;
                }
                // unsubscribe
                if (unsubscribeIf != null && unsubscribeIf.supply()) {
                    EventManager.this.unsubscribe( Collections.singleton( EventHandlerInfoImpl.this ) );
                    return;
                }
                // perform: listener
                if (handler instanceof EventListener) {
                    // TODO check param type  -- Optional<ClassInfo<Object>> cli = ClassInfo.of( handler );
                    ((EventListener)handler).handle( ev );
                    return;
                }
                // perform: annotated
                var cacheKey = ImmutablePair.of( handler.getClass(), ev.getClass() );
                var method = methodCache.computeIfAbsent( cacheKey, __ -> {
                    var cli = ClassInfo.of( handler );
                    for (MethodInfo m : cli.methods()) {
                        Opt<EventHandler> a = m.annotation( EventHandler.class );
                        if (a.isPresent() && a.get().value().isInstance( ev )) {
                            LOG.debug( "Method cache: %s + 1", methodCache.size() );
                            Assert.that( methodCache.size() < 64, "EventManager: methodCache exceeds 64 entries. Ok?" );
                            return m;
                        }
                    }
                    throw new IllegalStateException( "Handler is neither an EventListener nor annotated: "
                            + cli.simpleName() + " (" + ev.getClass().getSimpleName() + ")" );
                });
                try {
                    method.invoke( handler, ev );
                }
                catch (InvocationTargetException e) {
                    throw (RuntimeException)e.getTargetException();
                }
            }
            catch (Throwable e) {
                onError.accept( ev, e );
            }
        }

    }

}
