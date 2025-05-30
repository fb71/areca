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

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import areca.common.Platform;
import areca.common.Promise;
import areca.common.base.Consumer.RConsumer;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * Collecting chunks of events and deferred handling them all together.
 * Can be used to throttle event rate.
 *
 * @author Falko Bräutigam
 */
public class EventCollector<T extends EventObject> {

    private static final Log LOG = LogFactory.getLog( EventCollector.class );

    protected int           delay;

    protected List<T>       events = new ArrayList<>( 128 );

    private Promise<Void>   async;


    public EventCollector( int delay ) {
        this.delay = delay;
    }


    public EventCollector<T> delay( @SuppressWarnings( "hiding" ) int delay ) {
        this.delay = delay;
        return this;
    }


    public void collect( T ev, RConsumer<List<T>> handler ) {
        events.add( ev );
        if (async == null) {
            async = Platform.schedule( delay, () -> {
                LOG.debug( "Publishing: %s events", events.size() );
                handler.accept( events );
                events = new ArrayList<>( 128 );
                async = null;
                return null;
            });
        }
    }

}
