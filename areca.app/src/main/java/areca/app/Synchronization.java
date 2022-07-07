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

import java.util.concurrent.TimeUnit;

import org.polymap.model2.runtime.UnitOfWork;

import areca.app.model.ModelUpdateEvent;
import areca.app.service.SyncableService;
import areca.app.service.SyncableService.SyncContext;
import areca.app.service.SyncableService.SyncType;
import areca.common.Platform;
import areca.common.ProgressMonitor;
import areca.common.Promise;
import areca.common.Timer;
import areca.common.base.Sequence;
import areca.common.event.EventManager;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * Starts {@link SyncType#BACKGROUND} sync services and handles periodic
 * {@link SyncType#INCREMENTAL} runs.
 *
 * @author Falko Bräutigam
 */
class Synchronization {

    private static final Log LOG = LogFactory.getLog( Synchronization.class );

    protected static final int  MAX_INCREMENTAL_DELAY = 30 * 60 * 1000;

    protected static final int  MIN_INCREMENTAL_DELAY = 1 * 60 * 1000;

    protected ArecaApp   app;

    protected int        incrementalDelay = MAX_INCREMENTAL_DELAY;

    protected Timer      lastIncrementalRun = Timer.start();


    public Synchronization( ArecaApp app ) {
        this.app = app;

        // start INCREMENTAL/BACKGROUND services (after we have the monitor UI)
        Platform.schedule( 5000, () -> {
            incremental();
            start( SyncType.BACKGROUND );
        });

        // FULL sync after settings have been changed
        var settingsEntities = Sequence.of( ArecaApp.SETTINGS_ENTITY_TYPES ).map( info -> info.type() ).toSet();
        EventManager.instance()
                .subscribe( (ModelUpdateEvent ev) -> {
                    for (var entityType : settingsEntities) {
                        if (!ev.entities( entityType ).isEmpty()) {
                            LOG.info( "Settings: %s changed", entityType.getSimpleName() );
                            start( SyncType.FULL );
                        }
                    }
                })
                .performIf( ModelUpdateEvent.class::isInstance );
    }


    protected void incremental() {
        if (lastIncrementalRun.elapsed( TimeUnit.MILLISECONDS ) > incrementalDelay) {
            start( SyncType.INCREMENTAL );
            lastIncrementalRun.restart();
            incrementalDelay = MAX_INCREMENTAL_DELAY;
        }

        Platform.schedule( 5*1000, () -> {
            incremental();
        });
    }


    /**
     * Force next {@link SyncType#INCREMENTAL} sync to run within the given delay.
     */
    public void forceIncremental( int delay ) {
        incrementalDelay = Math.min( MAX_INCREMENTAL_DELAY, delay );
    }


    public void start( SyncType type ) {
        for (var service : app.services( SyncableService.class ).asCollection()) {
            app.scheduleModelUpdate( uow -> {
                var ctx = new SyncContext() {
                    ProgressMonitor monitor;
                    @Override public ProgressMonitor monitor() {
                        return monitor != null ? monitor : (monitor = app.newAsyncOperation());
                    }
                    @Override public UnitOfWork unitOfWork() {
                        return uow;
                    }
                };
                return service.newSync( type, ctx )
                        .then( sync -> {
                            return sync != null ? sync.start() : Promise.completed( null );
                        })
                        .onSuccess( __ -> { if (ctx.monitor != null) ctx.monitor.done(); } )
                        .onError( __ -> { if (ctx.monitor != null) ctx.monitor.done(); } );
            });
        }
    }


}
