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

import areca.aws.XLogger;

/**
 *
 * @author Falko Bräutigam
 */
public class IndexRedirectHandler
        extends RequestHandler {

    private static final XLogger LOG = XLogger.get( IndexRedirectHandler.class );

    public static final IndexRedirectHandler INSTANCE = new IndexRedirectHandler();

    protected IndexRedirectHandler() {
        super( notYetCommitted
                .and( probe -> probe.proxyPath.redirect != null )
                // redirect just "/", not *everything* starting with "/"
                .and( probe -> probe.proxyPath.path.equals( probe.request.getPathInfo() ) ) );
    }

    @Override
    public void handle( Probe probe ) throws Exception {
        probe.response.sendRedirect( probe.proxyPath.redirect );
    }

}
