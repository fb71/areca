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
import static org.apache.commons.io.FilenameUtils.wildcardMatch;

import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.FilenameUtils;

import areca.aws.XLogger;

/**
 *
 * @author Falko Bräutigam
 */
public class FuckOffHandler
        extends RequestHandler {

    private static final XLogger LOG = XLogger.get( FuckOffHandler.class );

    public static List<String> RULES = Arrays.asList(
            "*/.env",
            "*/*.asp?",
            "*/*.php",
            "/wp-includes/*",
            "/wp-content/*" );


    protected FuckOffHandler() {
        super( notYetCommitted );
    }


    @Override
    public void handle( Probe probe ) throws Exception {
        var path = probe.request.getPathInfo();

        for (String rule : RULES) {
            if (FilenameUtils.wildcardMatch( path, rule )) {
                probe.response.sendError( 404, "Fuck Off!!!" );
                LOG.info( "BLOCKED: %s (%s)", path, rule );
            }
        }
    }


    // test ***********************************************

    public static void main( String... args ) {
        LOG.info( "Match: %s", wildcardMatch( "/first/irgendwas/mist.aspx", "*/*.asp?" ) );
        LOG.info( "Match: %s", wildcardMatch( "/wp-includes/images/include.php", "/wp-includes/*" ) );
        LOG.info( "Match: %s", wildcardMatch( "/.env", "*/.env" ) );
    }
}
