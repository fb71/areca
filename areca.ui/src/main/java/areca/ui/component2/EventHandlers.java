/*
 * Copyright (C) 2022-2024, the @authors. All rights reserved.
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
package areca.ui.component2;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EventObject;
import org.apache.commons.lang3.ArrayUtils;

import areca.common.Platform;
import areca.common.Promise;
import areca.common.Session;
import areca.common.Timer;
import areca.common.base.Consumer.RConsumer;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.Property.PropertyChangedEvent;

/**
 * Async delivering of UI update events (fired by {@link Property}) via
 * {@link Platform#requestAnimationFrame(areca.common.base.Consumer.RConsumer)}.
 *
 * @see Property
 * @see UIComponentEvent
 * @author Falko Bräutigam
 */
@SuppressWarnings("rawtypes")
public class EventHandlers {

    private static final Log LOG = LogFactory.getLog( EventHandlers.class );

    private static final int        INIT_QUEUE_CAPACITY = 512;

    private static final int        MAX_TIME_PER_FRAME = 5;

    private static Deque<Event>     eventQueue = new ArrayDeque<>( INIT_QUEUE_CAPACITY );

    private static Promise<Void>    async;


    public static EventHandlers create() {
        return Session.createInstance( EventHandlers.class );
    }


    // instance *******************************************

    // copy-on-write
    protected RConsumer[]           handlers = {};


    public <T extends EventObject> void add( RConsumer<T> consumer ) {
        handlers = ArrayUtils.add( handlers, consumer );
    }


    public void fireEvent( PropertyChangedEvent<Object> ev ) {
        if (eventQueue.isEmpty()) {
            async = Platform.requestAnimationFrame( ts -> processEvents( ts ) );
        }
        eventQueue.addLast( new Event( ev, handlers ) );
    }


    @SuppressWarnings("unchecked")
    protected static void processEvents( Double timestamp ) {
        async = null;
        var t = Timer.start();
        var count = 0;
        while (!eventQueue.isEmpty()
                && t.elapsedMillis() < MAX_TIME_PER_FRAME
                /*&& count < 30*/) {
            count ++;
            var queued = eventQueue.pollFirst();
            for (var handler : queued.handlers) {
                try {
                    handler.accept( queued.ev );
                }
                catch (Exception e) {
                    throw (RuntimeException)e;
                }
            }
        }
        async = !eventQueue.isEmpty() && async == null
                ? Platform.requestAnimationFrame( ts -> processEvents( ts ) )
                : null;

        LOG.info( "Processed: %s, Remaining: %s, Time: %s", count, eventQueue.size(), t.elapsedHumanReadable() );
    }


    /**
     *
     */
    private static class Event {

        public EventObject            ev;

        public RConsumer[]            handlers;

        protected Event( EventObject ev, RConsumer[] handlers ) {
            this.ev = ev;
            this.handlers = handlers;
        }
    }

}
