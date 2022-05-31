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

import org.apache.commons.lang3.mutable.MutableInt;

import org.polymap.model2.Entity;
import org.polymap.model2.ManyAssociation;
import org.polymap.model2.Nullable;
import org.polymap.model2.Property;
import org.polymap.model2.Queryable;
import org.polymap.model2.runtime.Lifecycle;
import org.polymap.model2.runtime.config.Mandatory;

import areca.common.Promise;
import areca.common.event.EventListener;
import areca.common.event.EventManager;
import areca.common.event.EventManager.EventHandlerInfo;
import areca.common.reflect.RuntimeInfo;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class Anchor
        extends Entity
        implements Lifecycle {

    public static final AnchorClassInfo info = AnchorClassInfo.instance();

    public static Anchor TYPE;

    @Mandatory
    @Queryable
    public Property<String>         name;

    public ManyAssociation<Message> messages;

    @Nullable
    @Queryable
    public Property<String>         storeRef;


    public Promise<Integer> unreadMessagesCount() {
        // XXX a ComputedProperty would be cached?!
        return messages.fetch()
                .reduce( new MutableInt(), (count,opt) -> opt.ifPresent( msg -> count.add( msg.unread.get() ? 1 : 0 ) ) )
                .map( count -> count.getValue() );
    }


    @Override
    public void onLifecycleChange( State state ) {
        EventManager.instance().publish( new EntityLifecycleEvent( this, state ) );
    }


    public EventHandlerInfo onLifecycle( State state, EventListener<EntityLifecycleEvent> l ) {
        return EventManager.instance().subscribe( l )
                .performIf( ev -> ev instanceof EntityLifecycleEvent
                        && ((EntityLifecycleEvent)ev).state == state
                        && ev.getSource() == Anchor.this );
    }

}
