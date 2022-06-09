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
import java.util.List;
import java.util.Set;

import org.polymap.model2.Entity;
import org.polymap.model2.runtime.UnitOfWork;

import areca.app.ArecaApp;
import areca.common.Promise;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * Fired by {@link ArecaApp} when entities were modified and after the main
 * {@link UnitOfWork} was refreshed.
 *
 * @author Falko Bräutigam
 */
public class ModelSubmittedEvent
        extends EventObject {

    private static final Log LOG = LogFactory.getLog( ModelSubmittedEvent.class );

    private List<EntityLifecycleEvent>      events;


    public ModelSubmittedEvent( ArecaApp source, List<EntityLifecycleEvent> collected ) {
        super( source );
        this.events = collected;
    }


    public <R extends Entity> Set<Object> entities( Class<R> type ) {
        return Sequence.of( events )
                .map( ev -> ev.getSource() )
                .filter( entity -> type.isInstance( entity ) )
                .map( entity -> entity.id() )
                .asSet();
    }


    public <R extends Entity> Promise<Set<R>> entities( Class<R> type, UnitOfWork uow ) {
        throw new RuntimeException( "..." );
        //return Sequence.of( entities( type ) ).map( id -> uow.entity( type, id ) ).asCollection();
    }

}
