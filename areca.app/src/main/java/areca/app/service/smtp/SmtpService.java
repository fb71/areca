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
package areca.app.service.smtp;

import areca.app.service.Service;
import areca.app.service.TransportService;
import areca.common.Promise;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public class SmtpService
        extends Service
        implements TransportService {

    private static final Log LOG = LogFactory.getLog( SmtpService.class );

    @Override
    public String label() {
        return "EMail";
    }

    @Override
    public Promise<Transport> newTransport( String receipient, TransportContext ctx ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

    /**
     *
     */
    protected class SmtpTransport
            extends Transport {

        @Override
        public Promise<Sent> send( String text ) {
            // XXX Auto-generated method stub
            throw new RuntimeException( "not yet implemented." );
        }
    }
}
