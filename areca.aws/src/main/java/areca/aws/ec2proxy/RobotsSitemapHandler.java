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
public class RobotsSitemapHandler
        extends RequestHandler {

    private static final XLogger LOG = XLogger.get( RobotsSitemapHandler.class );

    public RobotsSitemapHandler() {
        super( notYetCommitted );
    }

    @Override
    public void handle( Probe probe ) throws Exception {
        if (probe.request.getPathInfo().equals( "/robots.txt" )) {
            probe.response.sendError( 404, "No robots.txt yet." );
            LOG.info( "No robots.txt: %s", probe.request.getPathInfo() );
        }
    }

}
