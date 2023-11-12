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

import areca.aws.XLogger;

/**
 *
 * @author Falko Bräutigam
 */
public class IndexRedirectHandler
        extends RequestHandler {

    private static final XLogger LOG = XLogger.get( IndexRedirectHandler.class );

    protected IndexRedirectHandler() {
        super( notYetCommitted.and( probe -> probe.proxyPath.redirect != null ) );
    }

    @Override
    public void handle( Probe probe ) throws Exception {
        var p = probe.proxyPath.path;
        if (probe.request.getPathInfo().equals( p ) ) {
            LOG.info( "Sending redirect: " + probe.proxyPath.redirect );
            probe.response.sendRedirect( probe.proxyPath.redirect );
        }
    }

}
