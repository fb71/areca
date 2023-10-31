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

import java.net.ConnectException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.Instant;

import areca.aws.XLogger;

/**
 *
 * @author Falko Bräutigam
 */
public class EnsureEc2InstanceHandler
        extends HttpHandler {

    private static final XLogger LOG = XLogger.get( EnsureEc2InstanceHandler.class );

    public enum Mode {
       WAIT, LOADING_PAGE
    }

    protected Mode mode;

    protected EnsureEc2InstanceHandler( Mode mode ) {
        super( notYetCommitted.and( ec2InstanceIsRunning.negate() ) );
        this.mode = mode;
    }


    @Override
    public void handle( Probe probe ) throws Exception {
        // expect not running
        probe.vhost.updateRunning( false, () -> {
            probe.aws.startInstance( probe.vhost.ec2id );
            waitForService( probe );
            return true;
        });
    }


    protected void waitForService( Probe probe ) throws Exception {
        for (var start = Instant.now(); Duration.between( start, Instant.now() ).compareTo( TIMEOUT_SERVICES_STARTUP ) < 0;) {
            try {
                StraightForwardHandler forward = new StraightForwardHandler();
                var response = forward.sendRequest( probe );
                // do not send error back to the client
                // assuming that error status signals that service is not yet fully started
                if (response.statusCode() < 400) {
                    forward.handleResponse( probe, response );
                }
                else {
                    LOG.info( "Response: status code = %s", response.statusCode() );
                    throw new HttpTimeoutException( "Response: status code =" + response.statusCode() );
                }
                return;
            }
            catch (HttpTimeoutException|ConnectException e) {
                LOG.info( "Waiting for connection: %s/%s", probe.vhost.hostnames.get( 0 ), probe.vhost.ec2id );
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

        protected AfterError( Mode mode ) {
            super( mode );
            predicate = notYetCommitted.and( probe -> probe.error != null );
        }

        @Override
        public void handle( Probe probe ) throws Exception {
            // force, no matter what current state is
            probe.vhost.updateRunning( null, () -> {
                probe.aws.startInstance( probe.vhost.ec2id );
                waitForService( probe );
                return true;
            });
        }
    }

}
