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
package areca.app;

import java.util.ArrayDeque;
import java.util.Deque;

import org.polymap.model2.Entity;
import org.polymap.model2.runtime.Lifecycle.State;
import org.polymap.model2.runtime.UnitOfWork;

import areca.app.model.EntityLifecycleEvent;
import areca.app.model.ModelUpdateEvent;
import areca.common.Assert;
import areca.common.Promise;
import areca.common.Scheduler.Priority;
import areca.common.Timer;
import areca.common.base.Function.RFunction;
import areca.common.base.Sequence;
import areca.common.event.EventCollector;
import areca.common.event.EventManager;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * Prevent concurrent model updates by serializing all write accesses. And, refresh
 * model(s) after update (event).
 *
 * @author Falko Bräutigam
 */
public class ModelUpdates {

    private static final Log LOG = LogFactory.getLog( ModelUpdates.class );

    public static final int DEFAULT_MODEL_UPDATE_EVENT_DELAY = 3000;

    private ArecaApp        app;

    private EventCollector<EntityLifecycleEvent> modelUpdateEventCollector =
            new EventCollector<EntityLifecycleEvent>( DEFAULT_MODEL_UPDATE_EVENT_DELAY );

    /**
     * The first element/update is currently running.
     */
    private Deque<RFunction<UnitOfWork,Promise<?>>> modelUpdates = new ArrayDeque<>();

    private Timer           timer = Timer.start();


    ModelUpdates( ArecaApp app ) {
        this.app = app;
        startCollectingModelEvents();
    }


    /**
     * Schedules the given update operation to be executed as soon as possible. The
     * update operation receives its own {@link UnitOfWork}. The resulting {@link Promise}
     * MUST produce just ONE result.
     *
     * @param update The operation. Must produce just one result.
     */
    public void schedule( RFunction<UnitOfWork,Promise<?>> update ) {
        modelUpdates.addLast( update );

        // no other update currently running?
        if (modelUpdates.size() == 1) {
            startNextUpdate();
        }
    }


    protected void modelUpdateCompleted( RFunction<UnitOfWork,Promise<?>> expected ) {
        LOG.info( "Update done. (%s)", timer.elapsedHumanReadable() );
        timer.restart();
        var first = modelUpdates.pollFirst();
        Assert.notNull( first, "Model update operations MUST produce exactly ONE result in the Promise!" );
        Assert.isSame( first, expected );
        startNextUpdate( );
    }


    protected void startNextUpdate() {
        timer.restart();
        var first = modelUpdates.peekFirst();
        if (first != null) {
            try {
                LOG.info( "Starting (next) update operation..." );
                first.apply( app.repo.newUnitOfWork().setPriority( Priority.BACKGROUND ) )
                        .onSuccess( __ -> modelUpdateCompleted( first ) )
                        .onError( app.defaultErrorHandler() )
                        .onError( __ -> modelUpdateCompleted( first ) );
            }
            // first.apply() error (?!?)
            catch (Exception e) {
                //defaultErrorHandler().accept( e );
                modelUpdateCompleted( first );
            }
        }
    }


    protected void startCollectingModelEvents() {
        // model updates
        var appEntities = Sequence.of( ArecaApp.APP_ENTITY_TYPES ).map( info -> info.type() ).toList();
        EventManager.instance()
                .subscribe( (EntityLifecycleEvent ev) -> {
                    // no refresh needed if main UoW was submitted
                    if (ev.getSource().context.getUnitOfWork() == app.uow) {
                        return;
                    }
                    modelUpdateEventCollector.collect( ev, collected -> {
                        var mue = new ModelUpdateEvent( app, collected );
                        var ids = mue.entities( Entity.class );
                        LOG.info( "Refreshing: %s entities", ids.size() );
                        app.uow.refresh( ids ).onSuccess( __ -> {
                            EventManager.instance().publish( mue );
                        });
                    });
                })
                .performIf( EntityLifecycleEvent.class, ev ->
                            ev.state == State.AFTER_SUBMIT &&
                            appEntities.contains( ev.getSource().getClass() ))
                .unsubscribeIf( () -> !app.uow.isOpen() );

        // settings updates
        var collector2 = new EventCollector<EntityLifecycleEvent>( DEFAULT_MODEL_UPDATE_EVENT_DELAY );
        var settingsEntities = Sequence.of( ArecaApp.SETTINGS_ENTITY_TYPES ).map( info -> info.type() ).toList();
        EventManager.instance()
                .subscribe( (EntityLifecycleEvent ev) -> {
                    collector2.collect( ev, collected -> {
                        var mue = new ModelUpdateEvent( app, collected );
                        EventManager.instance().publish( mue );
                    });
                })
                .performIf( ev -> ev instanceof EntityLifecycleEvent
                        && ((EntityLifecycleEvent)ev).state == State.AFTER_SUBMIT
                        && settingsEntities.contains( ev.getSource().getClass() ));
    }
}
