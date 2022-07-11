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

import org.polymap.model2.Entity;

import areca.app.model.Address;
import areca.app.model.Message;
import areca.common.ProgressMonitor;
import areca.common.Promise;
import areca.common.base.Opt;

/**
 *
 * @author Falko Bräutigam
 */
public interface TransportService<S extends Entity> {

    /** */
    public interface TransportContext {
        public ProgressMonitor newMonitor();
        //public RSupplier<UnitOfWork> uowFactory;
    }

    public Class<S> transportSettingsType();

    /**
     * A new {@link Transport} object, or {@link Opt#absent()} if this service cannot
     * handle the given receipients.
     */
    public Opt<Transport> newTransport( Address address, TransportContext ctx, S settings );

    /**
     *
     */
    public abstract static class Transport {

        /**
         * Sends the given message.
         * <p/>
         * Default error handlers are attached by caller.
         */
        public abstract Promise<Sent> send( TransportMessage msg );
    }

    /**
     * A message that is to be sent via a {@link Transport}.
     */
    public static class TransportMessage {

        public Opt<Message> followUp = Opt.absent();

        public Address      receipient;

        public Opt<String>  threadSubject;

        public String       text;
    }

    /**
     *
     */
    public static class Sent {

        public TransportMessage     message;

        public String               from;
    }

}
