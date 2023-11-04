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

import static areca.aws.ec2proxy.HttpForwardServlet4.TIMEOUT_SERVICES_STARTUP;
import static areca.aws.ec2proxy.Predicates.ec2InstanceIsRunning;
import static areca.aws.ec2proxy.Predicates.notYetCommitted;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import java.io.InputStream;
import java.net.ConnectException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.Instant;

import org.apache.commons.io.IOUtils;

import areca.aws.XLogger;

/**
 *
 * @author Falko Bräutigam
 */
public class EnsureEc2InstanceHandler
        extends RequestHandler {

    private static final XLogger LOG = XLogger.get( EnsureEc2InstanceHandler.class );

    public enum Mode {
       WAIT, LOADING_PAGE
    }

    private static Map<VHost,Thread> pending = new ConcurrentHashMap<>();

    // instance *******************************************

    protected Mode mode;

    protected EnsureEc2InstanceHandler( Mode mode ) {
        super( notYetCommitted.and( ec2InstanceIsRunning.negate() ) );
        this.mode = mode;
    }


    @Override
    public void handle( Probe probe ) throws Exception {
        // send loading page
        if (mode == Mode.LOADING_PAGE
                && probe.request.getHeader( "Accept" ).contains( "text/html" )) {

            pending.computeIfAbsent( probe.vhost, __ -> {
                var t = new Thread( "Ec2InstanceStarter" ) {
                    @Override
                    public void run() {
                        try {
                            probe.vhost.updateRunning( false, __ -> {
                                probe.aws.startInstance( probe.vhost.ec2id );
                                // waitForService( probe ); // XXX
                                Thread.sleep( 8000 );
                                LOG.info( "Instance is ready: %s", getName() );
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
                };
                t.start();
                return t;
            });

            probe.response.setStatus( 200 );

            try (var in = Thread.currentThread().getContextClassLoader().getResourceAsStream( "loading.html" )) {
                IOUtils.copy( in, probe.response.getOutputStream() );
                probe.response.flushBuffer();
            }
        }

        // wait for response
        else {
            startInstance( probe, /*expect not running*/ false );
        }
    }


    protected void startInstance( Probe probe, Boolean expected ) throws Exception {
        probe.vhost.updateRunning( expected, __ -> {
            probe.aws.startInstance( probe.vhost.ec2id );

            var response = waitForService( probe );
            StraightForwardHandler.INSTANCE.handleResponse( probe, response );
            return true;
        });
    }


    protected HttpResponse<InputStream> waitForService( Probe probe ) throws Exception {
        for (var start = Instant.now(); Duration.between( start, Instant.now() ).compareTo( TIMEOUT_SERVICES_STARTUP ) < 0;) {
            try {
                StraightForwardHandler forward = new StraightForwardHandler();
                var response = forward.sendRequest( probe );
                // do not send error back to the client
                // assuming that error status signals that service is not yet fully started
                if (response.statusCode() < 400) {
                    return response;
                }
                else {
                    LOG.info( "Response: status = %s", response.statusCode() );
                    throw new HttpTimeoutException( "Response: status code =" + response.statusCode() );
                }
            }
            catch (HttpTimeoutException|ConnectException e) {
                LOG.info( "Waiting for connection: %s (%s) ()", probe.vhost.hostnames.get( 0 ), e );
                Thread.sleep( 3000 );
            }
        }
        throw new HttpConnectTimeoutException( "No connection after: " + TIMEOUT_SERVICES_STARTUP.toSeconds() + "s" );
    }


    /**
     *
     */
    public static class AfterError
            extends EnsureEc2InstanceHandler {

        protected AfterError( Mode mode ) {
            super( mode );
            predicate = notYetCommitted.and( probe -> probe.error != null );
        }

        @Override
        public void handle( Probe probe ) throws Exception {
            startInstance( probe, /*force, no matter what current state is*/ null );
        }
    }

}
