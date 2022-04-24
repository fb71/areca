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
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import areca.common.Assert;
import areca.common.base.Opt;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.MethodInfo;

/**
 *
 * @author falko
 */
public abstract class EventManager {

    private static final Logger LOG = Logger.getLogger( EventManager.class.getSimpleName() );

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
    private List<EventHandlerInfo>              handlers = Collections.emptyList();


    protected EventManager() {
        defaultOnError = (ev, e) -> {
            LOG.log( Level.WARNING, "Error during handling of event: " + ev, e );

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


    public EventHandlerInfo subscribe( Object annotatedOrListener ) {
        EventHandlerInfo newHandler = new EventHandlerInfo( annotatedOrListener );

        List<EventHandlerInfo> newHandlers = new ArrayList<>( handlers.size() + 1 );
        for (EventHandlerInfo cursor : handlers) {
            if (cursor.handler == annotatedOrListener) {
                throw new IllegalStateException( "Event handler already subscribed!" );
            }
            newHandlers.add( cursor );
        }
        newHandlers.add( newHandler );
        handlers = newHandlers;
        return newHandler;
    }


    protected void unsubscribe( EventHandlerInfo eventHandlerInfo ) {
        List<EventHandlerInfo> newHandlers = new ArrayList<>( handlers.size() - 1 );
        for (EventHandlerInfo cursor : handlers) {
            if (cursor != eventHandlerInfo) {
                newHandlers.add( cursor );
            }
        }
        handlers = newHandlers;
    }


    public EventHandlerInfo subscribe( EventListener<?> l ) {
        return subscribe( (Object)l );
    }


    protected int keyOf( Object annotatedOrListener ) {
        return System.identityHashCode( annotatedOrListener );
    }


    /**
     * Fire the given events.
     * <p>
     * The triggered event handlers may publish new events. The caller has to make
     * sure that any event queue can handle insert during iteration.
     *
     * @param events
     */
    protected void fireEvent( EventObject ev ) {
        for (EventHandlerInfo handler : handlers) {
            handler.perform( ev );
        }
    }


    /**
     * Fires the given event. Depending on the actual EventManager implementation
     * the event processing is done asynchronously.
     */
    public abstract /*Promise<Void>*/ void publish( EventObject ev );


    /**
     * Fires the given event and waits until the event is processed by all handlers.
     */
    public abstract void publishAndWait( EventObject ev );


    /**
     *
     */
    public class EventHandlerInfo {

        protected Object                    handler;

        protected EventPredicate            performIf;

        protected EventPredicate            disposeIf;

        protected BiConsumer<EventObject,Throwable> onError = defaultOnError;


        public EventHandlerInfo( Object handler ) {
            this.handler = notNull( handler );
        }


        public EventHandlerInfo performIf( @SuppressWarnings("hiding") EventPredicate performIf ) {
            Assert.notNull( performIf );
            this.performIf = this.performIf != null ? this.performIf.and( performIf ) : performIf;
            return this;
        }


        public EventHandlerInfo disposeIf( @SuppressWarnings("hiding") EventPredicate disposeIf ) {
            Assert.isNull( this.disposeIf );
            this.disposeIf = notNull( disposeIf );
            return this;
        }


        protected void perform( EventObject ev ) {
            try {
                // dispose
                if (disposeIf != null && disposeIf.test( ev )) {
                    EventManager.this.unsubscribe( EventHandlerInfo.this );
                    return;
                }
                // filter
                if (performIf != null && !performIf.test( ev )) {
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
                        m.invoke( handler, ev );
                        return;
                    }
                }
                throw new IllegalStateException( "handler is neither an EventListener nor annotated! " );
            }
            catch (Throwable e) {
                onError.accept( ev, e );
            }
        }

    }

}
