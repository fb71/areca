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

import static areca.aws.ec2proxy.HttpForwardServlet4.TIMEOUT_REQUEST;
import static areca.aws.ec2proxy.HttpForwardServlet4.TIMEOUT_SERVICES_STARTUP;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.Instant;

import areca.aws.XLogger;

/**
 * Makes sure that the EC2 instance {@link VHost#isRunning}.
 * <p>
 * When in {@link Mode#LOADING_PAGE} a loading.hml is send to text/html request,
 * while {@link StartInstanceThread} is starting the instance. All other request are
 * simply blocked until the instance is ready.
 *
 * @author Falko Bräutigam
 */
public class EnsureEc2InstanceHandler
        extends RequestHandler {

    private static final XLogger LOG = XLogger.get( EnsureEc2InstanceHandler.class );

    public enum Mode {
       WAIT, LOADING_PAGE
    }

    private static final Map<VHost,Thread> pending = new ConcurrentHashMap<>();

    // instance *******************************************

    protected Mode mode;

    public EnsureEc2InstanceHandler( Mode mode ) {
        super( notYetCommitted
                .and( probe -> isNotBlank( probe.vhost.ec2id() ) )
                .and( ec2InstanceIsRunning.negate() ) );
        this.mode = mode;
    }


    @Override
    public void handle( Probe probe ) throws Exception {
        // favicon
        var pathInfo = probe.request.getPathInfo();
        if (mode == Mode.LOADING_PAGE && pathInfo.startsWith( "/favicon" )) {
            probe.response.sendError( 404, "No favicon during load." );
        }
        // loading page
        else if (mode == Mode.LOADING_PAGE &&
                contains( probe.request.getHeader( "Accept" ), "text/html" )) {
            if (!pending.containsKey( probe.vhost )) {
                pending.computeIfAbsent( probe.vhost, __ -> new StartInstanceThread( probe ) );
            }
            sendResource( probe, 200, "loading3.html" );
        }
        // just wait for response
        else {
            startInstance( probe, /*expect not running*/ false );
        }
    }

    /**
     * Starts the EC2 instance, while the user sees loading.html.
     */
    protected class StartInstanceThread extends Thread {

        private Probe probe;

        public StartInstanceThread( Probe probe ) {
            super( "Ec2InstanceStarter" );
            this.probe = probe;
            start();
        }

        @Override
        public void run() {
            try {
                probe.vhost.updateRunning( false, ___ -> {
                    probe.aws.startInstance( probe.vhost.ec2id() );
                    Thread.sleep( 3000 );
                    try {
                        waitForService( probe );
                        LOG.info( "Instance is ready: %s", getName() );
                    }
                    catch (HttpTimeoutException e) {
                        LOG.info( "Instance not ready after timeout. Running anyway: %s", getName() );
                    }
                    return true;
                });
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                pending.remove( probe.vhost );
            }
        }
    }

    protected void startInstance( Probe probe, Boolean expected ) throws Exception {
        probe.vhost.updateRunning( expected, __ -> {
            probe.aws.startInstance( probe.vhost.ec2id() );

            waitForService( probe );

            var forward = StraightForwardHandler.instance.get();
            forward.handleResponse( probe, forward.sendRequest( probe ) );
            return true;
        });
    }


    /**
     * Sending ping requests without using the request from the probe, which is
     * already done.
     */
    protected void waitForService( Probe probe ) throws Exception {
        for (var start = Instant.now(); Duration.between( start, Instant.now() ).compareTo( TIMEOUT_SERVICES_STARTUP ) < 0;) {
            try {
                // wbv/mapzone has just "/" as path, which would give 404
                var ping = probe.proxyPath.forward + defaultString( probe.proxyPath.ping );
                LOG.info( "PING: %s", ping );
                var request = HttpRequest.newBuilder( new URI( ping ) ).timeout( TIMEOUT_REQUEST );
                var response = probe.http.send( request.build(), BodyHandlers.ofInputStream() );

                // do not send error back to the client
                // assuming that error status signals that service is not yet fully started
                var sc = response.statusCode();
                if (sc < SC_BAD_REQUEST || sc == SC_UNAUTHORIZED || sc == SC_FORBIDDEN) {
                    return;
                }
                else {
                    LOG.info( "Response: status = %s", response.statusCode() );
                    throw new HttpTimeoutException( "Response: status code =" + response.statusCode() );
                }
            }
            catch (HttpTimeoutException|ConnectException e) {
                LOG.info( "Waiting for connection: %s (%s) ()", probe.vhost.hostnames().get( 0 ), e );
                Thread.sleep( 2000 );
            }
        }
        throw new HttpConnectTimeoutException( "No connection after: " + TIMEOUT_SERVICES_STARTUP.toSeconds() + "s" );
    }


    /**
     *
     */
    public static class AfterError
            extends EnsureEc2InstanceHandler {

        public AfterError( Mode mode ) {
            super( mode );
            predicate = notYetCommitted
                    .and( probe -> isNotBlank( probe.vhost.ec2id() ) )
                    .and( probe -> probe.error != null );
        }

        @Override
        public void handle( Probe probe ) throws Exception {
            startInstance( probe, /*force, no matter what current state is*/ null );
        }
    }

}
