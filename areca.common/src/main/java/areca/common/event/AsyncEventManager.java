/*
 * Copyright (C) 2020, the @authors. All rights reserved.
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

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import areca.common.Platform;
import areca.common.Promise;
import areca.common.Timer;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * Async delivering events via {@link Platform#async(Runnable)}.
 *
 * @author Falko Bräutigam
 */
public class AsyncEventManager
        extends EventManager {

    private static final Log LOG = LogFactory.getLog( AsyncEventManager.class );

    private static final int        INIT_QUEUE_CAPACITY = 128;

    private List<Event>             eventQueue = null;

    private Promise<Void>           async;


    public AsyncEventManager() {
        new ExpungeThread().run();
    }


    @Override
    public void publish( EventObject ev ) {
        publish2( ev );
    }


    @Override
    public Promise<Void> publish2( EventObject ev ) {
        if (eventQueue == null) {
            eventQueue = new ArrayList<>( INIT_QUEUE_CAPACITY );

            async = Platform.async( () -> {
                // handlers itself can publish events
                var stable = eventQueue;
                eventQueue = null;
                async = null;

                // TODO check if queue is to big or computation takes to long
                var handlersBefore = handlers.size();
                Timer t = Timer.start();
                for (Event queued : stable) {
                    for (EventHandlerInfo handler : queued.handlers) {
                        handler.perform( queued.ev );
                    }
                }
                LOG.debug( "Queued: %s - Handlers: %s (%s) - %s", stable.size(), handlers.size(), handlersBefore, t.elapsedHumanReadable() );
                return null;
            });
        }
        eventQueue.add( new Event( ev, handlers ) );
        return async;
    }


    /**
     *
     */
    private static class Event {

        public EventObject              ev;

        public List<EventHandlerInfo>   handlers;

        protected Event( EventObject ev, List<EventHandlerInfo> handlers ) {
            this.ev = ev;
            this.handlers = handlers;
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
