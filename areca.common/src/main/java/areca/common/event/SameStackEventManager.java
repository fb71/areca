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

import java.util.EventObject;

import areca.common.Promise;
import areca.common.base.BiConsumer;
import areca.common.base.Consumer;
import areca.common.base.Consumer.RConsumer;

/**
 * Simple {@link EventManager} implementation that executes event handlers
 * synchronously on the stack of the {@link #publish(java.util.EventObject)}
 * call.
 *
 * @author Falko Bräutigam
 */
public class SameStackEventManager
        extends EventManager {


    @Override
    public void publish( EventObject ev ) {
        for (EventHandlerInfo handler : handlers) {
            handler.perform( ev );
        }
    }


    @Override
    public Promise<Void> publish2( EventObject ev ) {
        publish( ev );

        return new Promise.Completable<>() {
            { complete( null ); }

            @Override
            public <E extends Exception> Promise<Void> onSuccess( Consumer<Void,E> consumer ) {
                return onSuccess( (promise,value) -> consumer.accept( value ) );
            }

            @Override
            public <E extends Exception> Promise<Void> onSuccess( BiConsumer<HandlerSite,Void,E> consumer ) {
                var site = new HandlerSite() {
                    @Override public void cancel() { throw new RuntimeException( "not yet implemented." ); }
                    @Override public boolean isCanceled() { throw new RuntimeException( "not yet implemented." ); }
                    @Override public boolean isComplete() { return true; }
                    @Override public int index() { throw new RuntimeException( "not yet implemented." ); }
                };
                try {
                    consumer.accept( site, null );
                }
                catch (Exception e) {
                    throw (RuntimeException)e; // XXX
                }
                return this;
            }

            @Override
            public Promise<Void> onError( RConsumer<Throwable> consumer ) {
                //throw new UnsupportedOperationException( "Event errors are catched by the handlers." );
                return this;
            }
        };
    }

}
