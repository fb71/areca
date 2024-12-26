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
package areca.aws.logs;

import static areca.aws.ec2proxy.HttpForwardServlet4.LOG_REQUEST;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import areca.aws.XLogger;
import areca.aws.ec2proxy.HttpForwardServlet4;
import areca.aws.ec2proxy.OnCommitResponseWrapper;
import areca.aws.ec2proxy.RequestHandler;

/**
 *
 * @author Falko Bräutigam
 */
public class LogRequestHandler
        extends RequestHandler {

    private static final XLogger LOG = XLogger.get( LogRequestHandler.class );


    public LogRequestHandler() {
        super( notYetCommitted );
    }


    @Override
    public void handle( Probe probe ) throws Exception {
        probe.ev = HttpRequestEvent.prepare( probe.request, probe.response );
        LOG.info( "%s %s ?%s", probe.request.getMethod(), probe.ev.url, defaultIfEmpty( probe.request.getQueryString(), "-" ) );

        probe.response = new OnCommitResponseWrapper( probe.response, error -> {
            probe.ev.error = error;
            probe.ev.vhost = probe.vhost.hostnames().get( 0 );
            probe.ev.complete();
            HttpForwardServlet4.logs.publish( LOG_REQUEST, probe.ev );
        });
    }

}
