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

import static areca.app.service.SyncableService.SyncType.BACKGROUND;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.polymap.model2.Entity;
import org.polymap.model2.runtime.UnitOfWork;

import areca.app.model.ModelUpdateEvent;
import areca.app.service.SyncableService;
import areca.app.service.SyncableService.Sync;
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
public class Synchronization {

    private static final Log LOG = LogFactory.getLog( Synchronization.class );

    protected static final int  MAX_INCREMENTAL_DELAY = 30 * 60 * 1000;

    protected static final int  MIN_INCREMENTAL_DELAY = 1 * 60 * 1000;

    protected ArecaApp      app;

    protected int           incrementalDelay = MAX_INCREMENTAL_DELAY;

    protected Timer         lastIncrementalRun = Timer.start();

    protected List<Sync>    background = new ArrayList<>();


    @SuppressWarnings("unchecked")
    public Synchronization( ArecaApp app ) {
        this.app = app;

        // start INCREMENTAL and BACKGROUND services (after we have the monitor UI)
        Platform.schedule( 5000, () -> {
            incremental();
            restartBackground();
        });

        // settings have been changed
        var settingsTypes = Sequence.of( ArecaApp.SETTINGS_ENTITY_TYPES ).map( info -> (Class<Entity>)info.type() ).toSet();
        EventManager.instance()
                .subscribe( (ModelUpdateEvent ev) -> {
                    // FULL
                    LOG.info( "Settings changed -> FULL sync" );
                    startFull();

                    // BACKGROUND
                    restartBackground();
                })
                .performIf( ModelUpdateEvent.class, ev -> ev.contains( settingsTypes ) );

        // model have been changed
        var entityTypes = Sequence.of( ArecaApp.APP_ENTITY_TYPES ).map( info -> (Class<Entity>)info.type() ).toSet();
        EventManager.instance()
                .subscribe( (ModelUpdateEvent ev) -> {
                    LOG.info( "Model changed -> OUTGOING sync" );
                    start( SyncType.OUTGOING, ev );
                })
                .performIf( ModelUpdateEvent.class, ev -> ev.contains( entityTypes ) );
    }


    /**
     * Periodically check/start {@link SyncType#INCREMENTAL} sync.
     */
    protected void incremental() {
        if (lastIncrementalRun.elapsed( TimeUnit.MILLISECONDS ) > incrementalDelay) {
            start( SyncType.INCREMENTAL, null );
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
    public void triggerIncremental( int delay ) {
        incrementalDelay = Math.min( MAX_INCREMENTAL_DELAY, delay );
    }


    public void startFull() {
        start( SyncType.FULL, null );
    }


    public void startOutgoing( ModelUpdateEvent outgoing ) {
        start( SyncType.OUTGOING, outgoing );
    }


    @SuppressWarnings("unchecked")
    protected void start( SyncType type, ModelUpdateEvent outgoing ) {
        // all services
        for (var service : app.services.ofType( SyncableService.class ).asCollection()) {
            var suow = app.settingsRepo.newUnitOfWork();
            Class<Entity> settingsType = service.syncSettingsType();
            suow.query( settingsType ).executeCollect()
                    .onSuccess( settingss -> {
                        // all settings
                        for (var settings : settingss) {
                            app.modelUpdates.schedule( uow -> {
                                var ctx = new SyncContext() {
                                    ProgressMonitor monitor;
                                    @Override public ProgressMonitor monitor() {
                                        return monitor != null ? monitor : (monitor = app.newAsyncOperation());
                                    }
                                    @Override public UnitOfWork unitOfWork() {
                                        return uow;
                                    }
                                    @Override
                                    public ModelUpdateEvent outgoing() {
                                        return outgoing;
                                    }
                                };
                                var sync = service.newSync( type, ctx, settings );
                                if (sync != null) {
                                    return sync.start()
                                            .onSuccess( __ -> { if (ctx.monitor != null) ctx.monitor.done(); } )
                                            .onError( __ -> { if (ctx.monitor != null) ctx.monitor.done(); } );
                                }
                                else {
                                    return Promise.completed( null );
                                }
                            });
                        }
                    });
        }
    }


    @SuppressWarnings("unchecked")
    protected void restartBackground() {
        for (var sync : background) {
            sync.dispose();
        }
        background.clear();

        // all services
        for (var service : app.services.ofType( SyncableService.class ).asCollection()) {
            var suow = app.settingsRepo.newUnitOfWork();
            Class<Entity> settingsType = service.syncSettingsType();
            suow.query( settingsType ).executeCollect()
                    .onSuccess( settingss -> {
                        // all settings
                        for (var settings : settingss) {
                            var ctx = new SyncContext() {
                                @Override public ProgressMonitor monitor() {
                                    return app.newAsyncOperation();
                                }
                                @Override public UnitOfWork unitOfWork() {
                                    throw new RuntimeException( "No default UoW in BACKGROUND Sync." );
                                }
                                @Override public ModelUpdateEvent outgoing() {
                                    throw new RuntimeException( "No outgoing in BACKGROUND Sync." );
                                }
                            };
                            var sync = service.newSync( BACKGROUND, ctx, settings );
                            if (sync != null) {
                                background.add( sync );
                                sync.start();
                            }
                        }
                    });
        }
    }

}
