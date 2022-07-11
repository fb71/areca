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
package areca.app.model;

import java.util.EventObject;

import org.polymap.model2.Entity;
import org.polymap.model2.runtime.EntityRuntimeContext.EntityStatus;
import org.polymap.model2.runtime.Lifecycle.State;
import org.polymap.model2.runtime.UnitOfWork;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public class EntityLifecycleEvent
        extends EventObject {

    private static final Log LOG = LogFactory.getLog( EntityLifecycleEvent.class );

    public State            state;

    public EntityStatus     entityStatus;

    public EntityLifecycleEvent( Entity source, State state ) {
        super( source );
        this.state = state;
        this.entityStatus = source.status();
    }

    /**
     * !Beware: The entity is probably from another {@link UnitOfWork}!
     */
    @Override
    public Entity getSource() {
        return (Entity)super.getSource();
    }

    @Override
    public String toString() {
        return String.format( "EntityLifecycleEvent[state=%s, type=%s]", state, getSource().getClass().getSimpleName() );
    }

}
