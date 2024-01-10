/*
 * Copyright (C) 2024, the @authors. All rights reserved.
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
package areca.ui.statenaction;

import java.util.EventObject;

import areca.common.Assert;

/**
 *
 * @author Falko Bräutigam
 */
public class StateChangeEvent extends EventObject  {

    public enum EventType {
        INITIALIZING, INITIALIZED, DISPOSING, DISPOSED
    }

    public EventType type;

    public StateChangeEvent( Object state, EventType type ) {
        super( Assert.notNull( state ) );
        this.type = Assert.notNull( type );
    }

    /**
     * The {@link State} originating this event.
     */
    @Override
    public Object getSource() {
        return super.getSource();
    }

}
