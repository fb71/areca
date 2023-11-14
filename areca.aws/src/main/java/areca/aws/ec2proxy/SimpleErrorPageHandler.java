/*
 * Copyright (C) 2023, the @authors. All rights reserved.
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
package areca.aws.ec2proxy;

import java.io.IOException;

import javax.servlet.http.HttpServletResponseWrapper;

import areca.aws.XLogger;

/**
 * Prevent container error page.
 *
 * @author Falko Bräutigam
 */
public class SimpleErrorPageHandler
        extends RequestHandler {

    private static final XLogger LOG = XLogger.get( SimpleErrorPageHandler.class );

    public SimpleErrorPageHandler() {
        super( notYetCommitted );
    }

    @Override
    public void handle( Probe probe ) throws Exception {
        probe.response = new HttpServletResponseWrapper( probe.response ) {
            @Override
            public void sendError( int sc, String msg ) throws IOException {
                LOG.info( "Sending error: %s - %s", sc, msg );
                setStatus( sc );
                try (var out = getOutputStream()) {
                    out.write( msg.getBytes( "UTF8" ) );
                }
            }
        };
    }

}
