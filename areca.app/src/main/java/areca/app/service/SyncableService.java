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

import areca.common.ProgressMonitor;
import areca.common.Promise;
import areca.common.base.Supplier.RSupplier;

/**
 *
 * @author Falko Bräutigam
 */
public interface SyncableService {

    /**
     *
     */
    public static class SyncContext {
        public ProgressMonitor          monitor;
        public RSupplier<UnitOfWork>    uowFactory;
    }

    public Sync newSync( SyncContext ctx );


    public abstract static class Sync {

        public abstract Promise<?> start();
    }
}
