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

import static areca.aws.ec2proxy.HttpForwardServlet4.BUFFER_SIZE;
import static areca.aws.ec2proxy.HttpForwardServlet4.COPY_SIZE;
import static areca.aws.ec2proxy.HttpForwardServlet4.FORBIDDEN_HEADERS;
import static areca.aws.ec2proxy.HttpForwardServlet4.METHODS_WITH_BODY;
import static areca.aws.ec2proxy.HttpForwardServlet4.TIMEOUT_REQUEST;

import java.util.function.Supplier;

import java.io.InputStream;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import areca.aws.Lazy;
import areca.aws.XLogger;

/**
 *
 * @author Falko Bräutigam
 */
public class StraightForwardHandler
        extends RequestHandler {

    private static final XLogger LOG = XLogger.get( StraightForwardHandler.class );

    /** */
    public static Supplier<StraightForwardHandler> instance = new Lazy<>( () -> new StraightForwardHandler() );

    public StraightForwardHandler() {
        super( notYetCommitted
                .and( probe -> probe.proxyPath.forward != null )
                .and( ec2InstanceIsRunning ) );
    }


    @Override
    public void handle( Probe probe ) throws Exception {
        var response = sendRequest( probe );
        handleResponse( probe, response );
    }


    protected HttpResponse<InputStream> sendRequest( Probe probe ) throws Exception {
        var forward = probe.proxyPath.forward
                + probe.request.getPathInfo().substring( probe.proxyPath.path.length() )
                + (probe.request.getQueryString() != null ? "?"+probe.request.getQueryString() : "" );
        probe.ev.forward = forward;

        LOG.info( "Sending request: %s", forward );
        var request = HttpRequest.newBuilder( new URI( forward ) );

        // XXX WBV "late/pending" request
        if (probe.request.getParameter( "cid" ) == null && probe.request.getParameter( "servicehandler" ) == null) {
            request.timeout( TIMEOUT_REQUEST );
        }

        // METHOD
        if (METHODS_WITH_BODY.contains( probe.request.getMethod() ) ) {
            var body = probe.requestBody.get();
            //LOG.info( "BODY: %s", new String( body, "UTF8" ) );
            request.method( probe.request.getMethod(), HttpRequest.BodyPublishers.ofByteArray( body ) );
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
                //LOG.info( "Header: %s: %s", name, Collections.list( probe.request.getHeaders( name ) ) );
            }
        });
        if (probe.request.getHeader( "Expect" ) != null) {
            request.expectContinue( true );
        }
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
        LOG.info( "Got response: %s", response.statusCode() );
        probe.response.setStatus( response.statusCode() );

        // headers
        response.headers().map().forEach( (name,values) -> {
            //LOG.debug( "Response Header: %s: %s", name, values );

            if (name.equalsIgnoreCase( "set-cookie" )) {
                LOG.info( "%s: %s", name, values );
                for (var value : values) {
                    var cookie = HttpCookie.parse( name + ":" + value ).get( 0 );
                    cookie.setVersion( 1 );
                    cookie.setPath( null );
                    LOG.info( "       -> %s", cookie.toString() );
                    probe.response.addHeader( name, cookie.toString() );
                }
            }
            else {
                values.forEach( value -> probe.response.addHeader( name, value ) );
            }
        });

//        var cm = probe.http.cookieHandler().get();
//        var cookies = cm.get( new URI( probe.redirect ), new HashMap<>() );
//        LOG.info( "################################### Response Cookie: %s", cookies );

        //LOG.warn( "Out buffer: %s", probe.response.getBufferSize() );
        //var start = System.nanoTime();
        probe.response.setBufferSize( BUFFER_SIZE );
        try (
            var in = response.body();
            var out = probe.response.getOutputStream();
        ){
            var buf = new byte[COPY_SIZE];
            for (int c = in.read( buf ); c > -1; c = in.read( buf )) {
                out.write( buf, 0, c );
                LOG.warn( "read: %s", c );
            }
            out.flush();
        }
        //LOG.warn( "   copy done: %s", (System.nanoTime()-start)/1000000 );
    }

}
