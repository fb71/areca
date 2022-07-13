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
package areca.app.service;

import org.polymap.model2.runtime.UnitOfWork;

import areca.app.ModelUpdates;
import areca.app.model.ModelUpdateEvent;
import areca.common.ProgressMonitor;
import areca.common.Promise;

/**
 *
 * @param <S> The settings type.
 * @author Falko Bräutigam
 */
public interface SyncableService<S> {

    public enum SyncType {
        /** Sync "everything" (but check existing) after settings have changed or created. */
        FULL,
        /** Periodic, fast check for "new" messages. And after startup. */
        INCREMENTAL,
        /** Permanently run in background Re-started when settings have been changed. */
        BACKGROUND,
        /** Executed when the model has changed. */
        OUTGOING
    }

    /**
     *
     */
    public interface SyncContext {

        public ProgressMonitor monitor();

        public UnitOfWork unitOfWork();

        public ModelUpdateEvent outgoing();
    }


    // instance *******************************************

    public Class<S> syncSettingsType();


    /**
     * A new Sync object, or null if there is nothing to sync.
     */
    public Sync newSync( SyncType syncType, SyncContext ctx, S settings );


    public abstract static class Sync {

        /**
         * Start this sync.
         * <p/>
         * Most sync types are started inside an
         * {@link ModelUpdates#schedule(RFunction) model update operation}. The resulting
         * {@link Promise} *must* produce just *one* result.
         * <p/>
         * Default error handlers are attached by caller.
         */
        public abstract Promise<?> start();

        public void dispose() {
            throw new RuntimeException( "Implement this for BACKGROUND sync!" );
        }
    }
}
