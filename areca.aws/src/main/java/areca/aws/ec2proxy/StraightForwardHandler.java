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

import static areca.aws.ec2proxy.HttpForwardServlet4.FORBIDDEN_HEADERS;
import static areca.aws.ec2proxy.HttpForwardServlet4.METHODS_WITH_BODY;
import static areca.aws.ec2proxy.HttpForwardServlet4.TIMEOUT_REQUEST;
import static areca.aws.ec2proxy.Predicates.ec2InstanceIsRunning;
import static areca.aws.ec2proxy.Predicates.notYetCommitted;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import areca.aws.XLogger;

/**
 *
 * @author Falko Bräutigam
 */
public class StraightForwardHandler
        extends HttpHandler {

    private static final XLogger LOG = XLogger.get( StraightForwardHandler.class );

    public StraightForwardHandler() {
        super( notYetCommitted.and( ec2InstanceIsRunning ) );
    }


    @Override
    public void handle( Probe probe ) throws Exception {
        var response = sendRequest( probe );
        handleResponse( probe, response );
    }


    protected HttpResponse<InputStream> sendRequest( Probe probe ) throws Exception {
        LOG.info( "Sending request: %s", probe.redirect );

        var request = HttpRequest.newBuilder( new URI( probe.redirect ) );

        // XXX WBV "late/pending" request
        if (probe.request.getParameter( "cid" ) == null && probe.request.getParameter( "servicehandler" ) == null) {
            request.timeout( TIMEOUT_REQUEST );
        }

        // METHOD
        if (METHODS_WITH_BODY.contains( probe.request.getMethod() ) ) {
            var in = probe.request.getInputStream();
            request.method( probe.request.getMethod(), HttpRequest.BodyPublishers.ofInputStream( () -> in ) );
        }
        else {
            request.method( probe.request.getMethod(), HttpRequest.BodyPublishers.noBody() );
        }

        // authentication?

        // headers
        probe.request.getHeaderNames().asIterator().forEachRemaining( name -> {
            if (!FORBIDDEN_HEADERS.contains( name.toLowerCase() )) {
                probe.request.getHeaders( name ).asIterator()
                        .forEachRemaining( value -> request.headers( name, value ) );
                //debug( "Header: %s: %s", name, Collections.list( probe.request.getHeaders( name ) ) );
            }
        });
        request.setHeader( "X-Forwarded-Host", probe.request.getServerName() );
        //debug( "XHeader: %s: %s", "X-Forwarded-Host", probe.request.getServerName() );
        request.setHeader( "X-Forwarded-Port", String.valueOf( probe.request.getServerPort() ) );
        //debug( "XHeader: %s: %s", "X-Forwarded-Port", probe.request.getServerPort() );
        request.setHeader( "X-Forwarded-Proto", probe.request.getScheme() );
        //debug( "XHeader: %s: %s", "X-Forwarded-Proto", probe.request.getScheme() );
        //request.setHeader( "X-Forwarded-For", req.getRemoteHost() );
        //debug( "XHeader: %s: %s", "X-Forwarded-For", req.getRemoteHost() );

        // send
        return probe.http.send( request.build(), BodyHandlers.ofInputStream() );
    }


    protected void handleResponse( Probe probe, HttpResponse<InputStream> response ) throws IOException {
        probe.response.setStatus( response.statusCode() );

        // headers
        response.headers().map().forEach( (name,values) -> {
            //debug( "Response Header: %s: %s", name, values );
            values.forEach( value -> probe.response.addHeader( name, value ) );
        });

        try (
            var in = response.body();
            var out = probe.response.getOutputStream();
        ) {
            var buf = new byte[4096];
            for (int c = in.read( buf ); c != -1; c = in.read( buf )) {
                out.write( buf, 0, c );
            }
            out.flush();
            probe.response.flushBuffer();
        }
    }

}