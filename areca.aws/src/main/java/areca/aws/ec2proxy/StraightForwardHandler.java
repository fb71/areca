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

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import org.apache.commons.io.IOUtils;

import areca.aws.XLogger;

/**
 *
 * @author Falko Bräutigam
 */
public class StraightForwardHandler
        extends HttpHandler {

    private static final XLogger LOG = XLogger.get( StraightForwardHandler.class );

    public static final StraightForwardHandler INSTANCE = new StraightForwardHandler();

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
            request.method( probe.request.getMethod(), HttpRequest.BodyPublishers.ofByteArray( probe.requestBody.get() ) );
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
                //LOG.debug( "Header: %s: %s", name, Collections.list( probe.request.getHeaders( name ) ) );
            }
        });
        //request.setHeader( "Host", "localhost:8080" );  //probe.request.getServerName() );
        //LOG.info( "XHeader: %s: %s", "Host", probe.request.getServerName() );
        request.setHeader( "X-Forwarded-Host", probe.request.getServerName() );
        //LOG.info( "XHeader: %s: %s", "X-Forwarded-Host", probe.request.getServerName() );
        request.setHeader( "X-Forwarded-Port", String.valueOf( probe.request.getServerPort() ) );
        //LOG.info( "XHeader: %s: %s", "X-Forwarded-Port", probe.request.getServerPort() );
        request.setHeader( "X-Forwarded-Proto", probe.request.getScheme() );
        //LOG.info( "XHeader: %s: %s", "X-Forwarded-Proto", probe.request.getScheme() );
        request.setHeader( "X-Forwarded-For", probe.request.getRemoteHost() );
        //LOG.debug( "XHeader: %s: %s", "X-Forwarded-For", probe.request.getRemoteHost() );
        request.setHeader( "X-Real-IP", probe.request.getRemoteHost() );
        //LOG.info( "XHeader: %s: %s", "X-Real-IP", probe.request.getRemoteHost() );

        // send
        return probe.http.send( request.build(), BodyHandlers.ofInputStream() );
    }


    protected void handleResponse( Probe probe, HttpResponse<InputStream> response ) throws Exception {
        probe.response.setStatus( response.statusCode() );

        // headers
        response.headers().map().forEach( (name,values) -> {
            //LOG.debug( "Response Header: %s: %s", name, values );
            values.forEach( value -> probe.response.addHeader( name, value ) );
        });

//        var cm = probe.http.cookieHandler().get();
//        var cookies = cm.get( new URI( probe.redirect ), new HashMap<>() );
//        LOG.info( "################################### Response Cookie: %s", cookies );

        IOUtils.copy( response.body(), probe.response.getOutputStream() );
        probe.response.flushBuffer();
    }

}
