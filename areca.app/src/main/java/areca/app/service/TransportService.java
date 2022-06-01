/*
 * Copyright (C) 2022, the @authors. All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software
 * Foundation; either version 3.0 of the License, or (at your option) any later
 * version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 */
package areca.app.service;

import areca.common.ProgressMonitor;
import areca.common.Promise;

/**
 *
 * @author Falko Bräutigam
 */
public interface TransportService {

    /** */
    public interface TransportContext {
        public ProgressMonitor newMonitor();
        //public RSupplier<UnitOfWork> uowFactory;
    }

    /**
     * A new {@link Transport} object, or null if this service cannot handle the
     * given receipients.
     * <p/>
     * The default implementation signals that there is no transport.
     */
    public default Promise<Transport> newTransport( String receipient, TransportContext ctx ) {
        return Promise.completed( null );
    }

    /**
     *
     */
    public abstract static class Transport {

        /**
         * Sends the given content.
         * <p/>
         * Default error handlers are attached by caller.
         */
        public abstract Promise<?> send( String text );
    }

}
