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

import java.util.EventObject;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import areca.common.Assert;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.MethodInfo;

/**
 *
 * @author falko
 */
public abstract class EventManager {

    private static final Logger LOG = Logger.getLogger( EventManager.class.getSimpleName() );

    private static final EventManager   INSTANCE = new SameStackEventManager();

    public static EventManager instance() {
        return INSTANCE;
    }


    // instance *******************************************

    public Consumer<Throwable>      defaultOnError = e -> LOG.log( Level.WARNING, "Error during event handling.", e );


    protected EventManager() {
    }


    public abstract EventHandlerInfo subscribe( Object annotatedOrListener );

    public EventHandlerInfo subscribe( EventListener<?> l ) {
        return subscribe( (Object)l );
    }

    protected abstract void unsubscribe( EventHandlerInfo eventHandlerInfo );


    /**
     * Fires the given event. Depending on the actual EventManager implementation
     * the event processing is done asynchronously.
     */
    public abstract void publish( EventObject ev );


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

        protected Consumer<Throwable>       onError = defaultOnError;


        public EventHandlerInfo( Object handler ) {
            this.handler = Assert.notNull( handler );
        }


        public EventHandlerInfo performIf( @SuppressWarnings("hiding") EventPredicate performIf ) {
            Assert.isNull( this.performIf );
            this.performIf = Assert.notNull( performIf );
            return this;
        }


        public EventHandlerInfo disposeIf( @SuppressWarnings("hiding") EventPredicate disposeIf ) {
            Assert.isNull( this.disposeIf );
            this.disposeIf = Assert.notNull( disposeIf );
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
                    // XXX check param type  -- Optional<ClassInfo<Object>> cli = ClassInfo.of( handler );
                    ((EventListener)handler).handle( ev );
                    return;
                }
                // perform: annotated
                Optional<ClassInfo<Object>> cli = ClassInfo.of( handler );
                if (cli.isPresent()) {
                    for (MethodInfo m : cli.get().methods()) {
                        Optional<EventHandlerAnnotationInfo> a = m.annotation( EventHandlerAnnotationInfo.INFO );
                        // FIXME check parameters
                        if (a.isPresent()) {
                            m.invoke( handler, ev );
                            return;
                        }
                    }
                }
                throw new IllegalStateException( "handler is neither an EventListener nor annotated! " );
            }
            catch (Throwable e) {
                onError.accept( e );
            }
        }

    }

}
