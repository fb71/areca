/*
 * Copyright (C) 2019, the @authors. All rights reserved.
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
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import java.lang.reflect.InvocationTargetException;

import areca.common.Assert;
import areca.common.Promise;
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

    private static EventManager     INSTANCE = new SameStackEventManager();

    public static EventManager instance() {
        return INSTANCE;
    }

    public static void setInstance( EventManager manager ) {
        INSTANCE = manager;
    }


    // instance *******************************************

    public BiConsumer<EventObject,Throwable>    defaultOnError;

    /** Copy-on-Write */
    protected List<EventHandlerInfo>            handlers = Collections.emptyList();


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
        EventHandlerInfo newHandler = new EventHandlerInfo( annotatedOrListener );

        List<EventHandlerInfo> newHandlers = new ArrayList<>( handlers.size() + 1 );
        // XXX arraycopy?
        for (EventHandlerInfo cursor : handlers) {
            if (cursor.handler == annotatedOrListener) {
                throw new IllegalStateException( "Event handler already subscribed!" );
            }
            newHandlers.add( cursor );
        }
        newHandlers.add( newHandler );
        Assert.isEqual( handlers.size()+1, newHandlers.size() );
        handlers = newHandlers;
        return newHandler;
    }


    protected void unsubscribe( Set<EventHandlerInfo> remove ) {
        var t = Timer.start();
        List<EventHandlerInfo> newHandlers = new ArrayList<>( handlers.size() - remove.size() );
        for (EventHandlerInfo handler : handlers) {
            if (!remove.contains( handler )) {
                newHandlers.add( handler );
            }
        }
        Assert.isEqual( handlers.size()-remove.size(), newHandlers.size() );
        LOG.info( "Expunged: %s, now: %s (%s) (%s)", remove.size(), newHandlers.size(),
                t.elapsedHumanReadable(), getClass().getSimpleName() );
        handlers = newHandlers;
    }


    protected int keyOf( Object annotatedOrListener ) {
        return System.identityHashCode( annotatedOrListener );
    }


//    /**
//     * Fire the given events.
//     * <p>
//     * The triggered event handlers may publish new events. The caller has to make
//     * sure that any event queue can handle insert during iteration.
//     *
//     * @param events
//     */
//    protected void fireEvent( EventObject ev ) {
//        for (EventHandlerInfo handler : handlers) {
//            handler.perform( ev );
//        }
//    }


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
    public class EventHandlerInfo {

        protected Object                    handler;

        protected BiConsumer<EventObject,Throwable>         onError = defaultOnError;

        protected Predicate<EventObject,RuntimeException>   performIf;

        protected Supplier<Boolean,RuntimeException>        unsubscribeIf;


        public EventHandlerInfo( Object handler ) {
            this.handler = notNull( handler );
        }


        @SuppressWarnings("hiding")
        public EventHandlerInfo performIf( RPredicate<EventObject> performIf ) {
            Assert.notNull( performIf );
            this.performIf = this.performIf != null ? this.performIf.and( performIf ) : performIf;
            return this;
        }


        @SuppressWarnings("hiding")
        public EventHandlerInfo unsubscribeIf( RSupplier<Boolean> unsubscribeIf ) {
            Assert.isNull( this.unsubscribeIf );
            this.unsubscribeIf = notNull( unsubscribeIf );
            return this;
        }


        protected void perform( EventObject ev ) {
            try {
                // filter
                if (performIf != null && !performIf.test( ev )) {
                    return;
                }
                // dispose
                if (unsubscribeIf != null && unsubscribeIf.supply()) {
                    EventManager.this.unsubscribe( Collections.singleton( EventHandlerInfo.this ) );
                    return;
                }
                // perform: listener
                if (handler instanceof EventListener) {
                    // TODO check param type  -- Optional<ClassInfo<Object>> cli = ClassInfo.of( handler );
                    ((EventListener)handler).handle( ev );
                    return;
                }
                // perform: annotated
                ClassInfo<Object> cli = ClassInfo.of( handler );
                for (MethodInfo m : cli.methods()) {
                    Opt<EventHandler> a = m.annotation( EventHandler.class );

                    if (a.isPresent() && a.get().value().isInstance( ev )) {
                        try {
                            m.invoke( handler, ev );
                            return;
                        }
                        catch (InvocationTargetException e) {
                            throw (RuntimeException)e.getTargetException();
                        }
                    }
                }
                throw new IllegalStateException( "Handler is neither an EventListener nor annotated: "
                            + cli.simpleName() + " (" + ev.getClass().getSimpleName() + ")" );
            }
            catch (Throwable e) {
                onError.accept( ev, e );
            }
        }

    }

}
