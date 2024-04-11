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

import static areca.common.Scheduler.Priority.BACKGROUND;
import java.util.EventObject;
import areca.common.Platform;
import areca.common.Promise;
import areca.common.Scheduler;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * Async delivering events via {@link Scheduler}.
 *
 * @author Falko Bräutigam
 */
public class IdleAsyncEventManager
        extends EventManager {

    private static final Log LOG = LogFactory.getLog( IdleAsyncEventManager.class );


    @Override
    public void publish( EventObject ev ) {
        publish2( ev );
    }


    @Override
    public Promise<Void> publish2( EventObject ev ) {
        var stableHandlers = handlers;
        return Platform.scheduler.schedule( BACKGROUND, () -> {
            for (var handler : stableHandlers) {
                handler.perform( ev );
            }
            LOG.debug( "Handlers: %s (%s)", stableHandlers.size(), handlers.size() );
            return null;
        });
    }

}
