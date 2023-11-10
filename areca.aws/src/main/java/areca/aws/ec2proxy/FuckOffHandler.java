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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
            "/.well-known*",
            "/joomla*",
            "*webalizer*",
            "*/.env",
            "*/*.asp?",
            "*/*.php",
            "/wp-includes/*",
            "/wp-content/*" );

    // instance *******************************************

    private static Map<String,Integer> blockedIPs = new ConcurrentHashMap<>();

    public FuckOffHandler() {
        super( notYetCommitted );
    }


    @Override
    public void handle( Probe probe ) throws Exception {
        var path = probe.request.getPathInfo();

        var ip = probe.request.getRemoteAddr();
        if (blockedIPs.containsKey( ip )) {
            var current = blockedIPs.computeIfPresent( ip, (__,count) -> count + 1 );
            probe.response.sendError( 404, "Double fuck off!!!" );
            LOG.info( "IP BLOCKED: %s (%s : %s)", path, ip, current );
            return;
        }

        for (String rule : RULES) {
            if (FilenameUtils.wildcardMatch( path, rule )) {
                probe.response.sendError( 404, "Fuck Off!!!" );
                LOG.info( "BLOCKED: %s (%s) (%s / %s)", path, rule, ip, blockedIPs.size() );
                blockedIPs.put( ip, 0 );
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
