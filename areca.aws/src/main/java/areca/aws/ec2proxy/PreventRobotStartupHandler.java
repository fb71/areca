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

import static java.util.Objects.requireNonNullElseGet;
import static org.apache.commons.lang3.StringUtils.contains;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import java.time.Instant;

import org.apache.commons.io.FilenameUtils;

import areca.aws.XLogger;

/**
 * Prevent robots from starting up the instance by sending a Captcha to text/html
 * request, allowing humans to pass. And redirect anything else to the start/base
 * URL. Aallow also all paths in {@link VHost.ProxyPath#allowedServices}.
 *
 * @author Falko Bräutigam
 */
public class PreventRobotStartupHandler
        extends RequestHandler {

    private static final XLogger LOG = XLogger.get( PreventRobotStartupHandler.class );

    private static Map<String,Instant> allowedIPs = new ConcurrentHashMap<>();

    public PreventRobotStartupHandler() {
        super( notYetCommitted
                .and( probe -> probe.vhost.ec2id != null )
                .and( ec2InstanceIsRunning.negate() )
                .and( probe -> !allowedIPs.containsKey( probe.request.getRemoteAddr() ) ) );
    }

    @Override
    public void handle( Probe probe ) throws Exception {
        // favicon
        var pathInfo = probe.request.getPathInfo();
        if (pathInfo.startsWith( "/favicon" )) {
            probe.response.sendError( 404, "No favicon during load." );
        }
        // Captcha page
        else if (contains( probe.request.getHeader( "Accept" ), "text/html" ) ) {
            // first page (no pending + no param)
            if (probe.request.getParameter( "v" ) == null) {
                sendResource( probe, 200, "loading-first.html" );
            }
            // loading (param is there)
            else {
                allowedIPs.put( probe.request.getRemoteAddr(), Instant.now() );
                probe.response.sendRedirect( pathInfo ); // remove v=? param
            }
        }
        // allowed services
        else {
            var allowedServices = requireNonNullElseGet( probe.proxyPath.allowedServices, () -> Collections.<String>emptyList() );
            for (String allowedPath : allowedServices) {
                var path = probe.request.getPathInfo();
                if (FilenameUtils.wildcardMatch( path, allowedPath )) {
                    allowedIPs.put( probe.request.getRemoteAddr(), Instant.now() );
                    return;
                }
            }
            // anything else -> redirect to captcha page
            probe.response.sendRedirect( probe.proxyPath.redirect != null
                    ? probe.proxyPath.redirect
                    : probe.proxyPath.path );
        }
    }
}
