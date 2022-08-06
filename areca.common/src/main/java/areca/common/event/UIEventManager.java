/*
 * Copyright (C) 2022, the @authors. All rights reserved.
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

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EventObject;
import java.util.List;

import areca.common.Platform;
import areca.common.Promise;
import areca.common.Promise.Completable;
import areca.common.Timer;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * Async delivering (UI) events via
 * {@link Platform#requestAnimationFrame(areca.common.base.Consumer.RConsumer)}.
 *
 * @author Falko Bräutigam
 */
public class UIEventManager
        extends EventManager {

    private static final Log LOG = LogFactory.getLog( UIEventManager.class );

    private static final int    INIT_QUEUE_CAPACITY = 1024;

    private static final int    MAX_TIME_PER_FRAME = 30;

    private Deque<Event>        eventQueue = new ArrayDeque<>( INIT_QUEUE_CAPACITY );

    private Promise<Void>       async;


    public UIEventManager() {
        new ExpungeThread().run();
    }


    @Override
    public void publish( EventObject ev ) {
        if (eventQueue.isEmpty()) {
            async = Platform.requestAnimationFrame( ts -> processEvents( ts ) );
        }
        eventQueue.addLast( new Event( ev, handlers ) );
    }


    @Override
    public Promise<Void> publish2( EventObject ev ) {
        if (eventQueue.isEmpty()) {
            async = Platform.requestAnimationFrame( ts -> processEvents( ts ) );
        }
        var queued = new Event( ev, handlers );
        eventQueue.addLast( queued );
        return queued.promise();
    }


    protected void processEvents( Double timestamp ) {
        async = null;
        var t = Timer.start();
        var count = 0;
        while (!eventQueue.isEmpty() && t.elapsed( MILLISECONDS ) < MAX_TIME_PER_FRAME) {
            var queued = eventQueue.pollFirst();
            count ++;
            for (EventHandlerInfo handler : queued.handlers) {
                handler.perform( queued.ev );
            }
            if (queued.promise != null) {
                queued.promise.complete( null );
            }
        }
        async = !eventQueue.isEmpty() && async == null
                ? Platform.requestAnimationFrame( ts -> processEvents( ts ) )
                : null;

        LOG.debug( "Processed: %s, Queued: %s - Handlers: %s - %s", count, eventQueue.size(), handlers.size(), t.elapsedHumanReadable() );
    }


    /**
     *
     */
    private static class Event {

        public EventObject              ev;

        public List<EventHandlerInfo>   handlers;

        public Completable<Void>        promise;

        protected Event( EventObject ev, List<EventHandlerInfo> handlers ) {
            this.ev = ev;
            this.handlers = handlers;
        }

        public Promise<Void> promise() {
            return promise = new Completable<>();
        }
    }


    /**
     *
     */
    private class ExpungeThread
            implements Runnable {

        @Override
        public void run() {
            var expunged = Sequence.of( handlers )
                    .filter( handler -> handler.unsubscribeIf != null && handler.unsubscribeIf.get() )
                    .toSet();

            if (!expunged.isEmpty()) {
                unsubscribe( expunged );
            }
            Platform.schedule( 3000, this );
        }
    }

}
