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

import static areca.aws.ec2proxy.Predicates.notYetCommitted;

import javax.servlet.http.HttpServletRequest;

import areca.aws.XLogger;

/**
 *
 * @author Falko Bräutigam
 */
public class SanityCheckHandler
        extends HttpHandler {

    private static final XLogger LOG = XLogger.get( SanityCheckHandler.class );

    protected SanityCheckHandler() {
        super( notYetCommitted );
    }

    @Override
    public void handle( Probe probe ) throws Exception {
        if (notYetCommitted.test( probe )) {
            LOG.warn( "No handler found!\n    %s\n    %s",
                    url( probe.request ), probe.redirect );
            probe.response.sendError( 500, "No handler found." );
        }
    }

    protected String url( HttpServletRequest req ) {
        return String.format( "%s://%s:%s/%s ?%s",
                req.getScheme(), req.getServerName(), req.getServerPort(), req.getRequestURI(), req.getQueryString() );
    }
}
