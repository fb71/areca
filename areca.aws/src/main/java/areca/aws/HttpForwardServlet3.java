/*
 * Copyright (C) 2021-2023, the @authors. All rights reserved.
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
package areca.aws;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * https://stackoverflow.com/questions/2793150/how-to-use-java-net-urlconnection-to-fire-and-handle-http-requests
 *
 * @author Falko Bräutigam
 */
public class HttpForwardServlet3
        extends HttpServlet {

    private static final List<String> METHODS_WITH_BODY = Arrays.asList("POST", "REPORT", "PUT");

    private static final List<String> FORBIDDEN_HEADERS = Arrays.asList( "host", "connection", "keep-alive",
            "proxy-authenticate", "proxy-authorization", "te", "trailers", "transfer-encoding", "upgrade",
            "content-length" );

    public static final Duration TIMEOUT_CONNECT = Duration.ofSeconds( 3 );
    public static final Duration TIMEOUT_REQUEST = Duration.ofSeconds( 10 );
    public static final Duration TIMEOUT_SERVICES_STARTUP = Duration.ofSeconds( 20 );

    // instance *******************************************

    private List<VHost>     vhosts;

    private SSLContext      sslContext;

    private HttpClient      http;


    @Override
    public void init() throws ServletException {
        log( getClass().getSimpleName() + " init..." );

        vhosts = VHost.readConfig();

        var noCookies = new CookieHandler() {
            @Override
            public Map<String,List<String>> get( URI uri, Map<String,List<String>> requestHeaders ) throws IOException {
                return Collections.emptyMap();
            }
            @Override
            public void put( URI uri, Map<String,List<String>> responseHeaders ) throws IOException {
            }
        };
        http = HttpClient.newBuilder().connectTimeout( TIMEOUT_CONNECT ).cookieHandler( noCookies ).build();

        sslContext = SSLUtils.trustAllSSLContext();
        HttpsURLConnection.setDefaultSSLSocketFactory( sslContext.getSocketFactory() );
        HttpsURLConnection.setDefaultHostnameVerifier( SSLUtils.DO_NOT_VERIFY );
    }


    @Override
    public void destroy() {
        log( getClass().getSimpleName() + ": destroy ..." );
        vhosts.forEach( vhost -> vhost.dispose() );
    }


    @Override
    protected void service( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException {
        try {
            doService( req, resp );
        }
        catch (ServletException|IOException e) {
            e.printStackTrace();
            throw e;
        }
        catch (NoSuchElementException e) {
            debug( "", e.getMessage() );
            resp.sendError( 404, e.getMessage() );
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException( e );
        }
    }


    protected void doService( HttpServletRequest req, HttpServletResponse resp ) throws Exception {
        debug( "URI: %s %s://%s:%s/%s ?%s", req.getMethod(),
                req.getScheme(), req.getServerName(), req.getServerPort(), req.getRequestURI(), req.getQueryString() );

        // vhost
        var vhost = vhosts.stream()
                .filter( _vhost -> _vhost.hostnames.contains( req.getServerName() ) ).findAny()
                .orElseThrow( () -> new NoSuchElementException( "No vhost found for server: " + req.getServerName() ) );

        // redirect
        var proxypath = vhost.proxypaths.stream()
                .filter( p -> req.getPathInfo().startsWith( p.path ) ).findAny()
                .orElseThrow( () -> new NoSuchElementException( "No proxypath found for: " + req.getPathInfo() ) );

        var redirect = proxypath.redirect
                + req.getPathInfo().substring( proxypath.path.length() )
                + (req.getQueryString() != null ? "?"+req.getQueryString() : "" );
        debug( "    -> %s", redirect );

        vhost.ensureRunning( () -> {
            var request = HttpRequest.newBuilder( new URI( redirect ) );
            // XXX WBV "late/pending" request
            if (req.getParameter( "cid" ) == null && req.getParameter( "servicehandler" ) == null) {
                request.timeout( TIMEOUT_REQUEST );
            }

            // METHOD
            if (METHODS_WITH_BODY.contains( req.getMethod() ) ) {
                var in = req.getInputStream();
                request.method( req.getMethod(), HttpRequest.BodyPublishers.ofInputStream( () -> in ) );
            }
            else {
                request.method( req.getMethod(), HttpRequest.BodyPublishers.noBody() );
            }

            // authentication?

            // headers
            req.getHeaderNames().asIterator().forEachRemaining( name -> {
                if (!FORBIDDEN_HEADERS.contains( name.toLowerCase() )) {
                    request.setHeader( name, req.getHeader( name ) );
                    debug( "Header: %s: %s", name, req.getHeader( name ) );
                }
            });
            request.setHeader( "X-Forwarded-Host", req.getServerName() );
            //debug( "XHeader: %s: %s", "X-Forwarded-Host", req.getServerName() );
            request.setHeader( "X-Forwarded-Port", String.valueOf( req.getServerPort() ) );
            //debug( "XHeader: %s: %s", "X-Forwarded-Port", req.getServerPort() );
            request.setHeader( "X-Forwarded-Proto", req.getScheme() );
            //debug( "XHeader: %s: %s", "X-Forwarded-Proto", req.getScheme() );
            request.setHeader( "X-Forwarded-For", req.getRemoteHost() );
            //debug( "XHeader: %s: %s", "X-Forwarded-For", req.getRemoteHost() );

            // send
            var response = http.send( request.build(), BodyHandlers.ofInputStream() );
            resp.setStatus( response.statusCode() );

            // headers
            response.headers().map().forEach( (name,values) -> {
                debug( "Response Header: %s: %s", name, values );
                if (name == null || name.equals( "WWW-Authenticate" )) {
                    // return 401 code but suppress the WWW-Authenticate header
                    // in order to prevent browser popup asking for credentials
                }
                else {
                    resp.addHeader( name, values.get( 0 ) ); // FIXME
                }
            });
            copyAndClose( response.body(), resp.getOutputStream() );
            return null;
        });
    }


    protected static void copyAndClose( InputStream in, OutputStream out) throws IOException {
        try (
            var _in = in;
            var _out = out
        ) {
            var buf = new byte[4096];
            for (int c = in.read( buf ); c != -1; c = in.read( buf )) {
                out.write( buf, 0, c );
            }
            out.flush();
        }
    }


    protected void debug( String msg, Object... args ) {
        System.out.println( args.length == 0 ? msg : String.format( msg, args ) );
    }

}
