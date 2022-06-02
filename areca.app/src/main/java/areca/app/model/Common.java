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
package areca.app.model;

import org.polymap.model2.Entity;
import org.polymap.model2.runtime.Lifecycle;
import areca.common.event.EventListener;
import areca.common.event.EventManager;
import areca.common.event.EventManager.EventHandlerInfo;

/**
 *
 * @author Falko Bräutigam
 */
public abstract class Common
        extends Entity
        implements Lifecycle {

    @Override
    public void onLifecycleChange( State state ) {
        EventManager.instance().publish( new EntityLifecycleEvent( this, state ) );
    }


    public EventHandlerInfo onLifecycle( State state, EventListener<EntityLifecycleEvent> l ) {
        return EventManager.instance().subscribe( l )
                .performIf( ev -> ev instanceof EntityLifecycleEvent
                        && ((EntityLifecycleEvent)ev).state == state
                        && ev.getSource() == Common.this );
    }

}
