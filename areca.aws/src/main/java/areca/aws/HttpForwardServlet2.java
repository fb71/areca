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
import java.util.List;
import java.util.NoSuchElementException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpUtils;

/**
 *
 * https://stackoverflow.com/questions/2793150/how-to-use-java-net-urlconnection-to-fire-and-handle-http-requests
 *
 * @author Falko Bräutigam
 */
public class HttpForwardServlet2
        extends HttpServlet {

    private static final List<String> METHODS_WITH_BODY = Arrays.asList("POST", "REPORT", "PUT");

    private static final List<String> FORBIDDEN_HEADERS = Arrays.asList( "Host", "Connection", "Keep-Alive",
            "Proxy-Authenticate", "Proxy-Authorization", "TE", "Trailers", "Transfer-Encoding", "Upgrade",
            "Content-Length" );

    private List<VHost>     vhosts;

    private SSLContext      sslContext;

    private HttpClient      http;


    @Override
    public void init() throws ServletException {
        log( "" + getClass().getSimpleName() + " init..." );

        vhosts = VHost.readConfig();

        http = HttpClient.newHttpClient();

        sslContext = SSLUtils.trustAllSSLContext();
        HttpsURLConnection.setDefaultSSLSocketFactory( sslContext.getSocketFactory() );
        HttpsURLConnection.setDefaultHostnameVerifier( SSLUtils.DO_NOT_VERIFY );
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
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    @SuppressWarnings("deprecation")
    protected void doService( HttpServletRequest req, HttpServletResponse resp ) throws Exception {
        debug( "URI: %s %s", req.getMethod(), HttpUtils.getRequestURL( req ) );

        // vhost
        var vhost = vhosts.stream()
                .filter( _vhost -> _vhost.hostnames.contains( req.getServerName() ) ).findAny()
                .orElseThrow( () -> new NoSuchElementException( "No vhost found for server: " + req.getServerName() ) );
        var redirect = vhost.proxypaths.get( 0 ).redirect + req.getPathInfo(); // XXX
        debug( "    -> %s", redirect );

        vhost.ensureRunning( () -> {
            var request = HttpRequest.newBuilder( new URI( redirect ) );

            // METHOD
            if (METHODS_WITH_BODY.contains( req.getMethod() ) ) {
                var in = req.getInputStream();
                request.method( req.getMethod(), HttpRequest.BodyPublishers.ofInputStream( () -> in ) );
            }
            else {
                request.method( req.getMethod(), HttpRequest.BodyPublishers.noBody() );
            }

            // authentication

            // headers
            req.getHeaderNames().asIterator().forEachRemaining( name -> {
                if (!FORBIDDEN_HEADERS.contains( name )) {
                    request.setHeader( name, req.getHeader( name ) );
                    //debug( "Header: %s: %s", name, req.getHeader( name ) );
                }
            });

            // send
            var response = http.send( request.build(), BodyHandlers.ofInputStream() );
            resp.setStatus( response.statusCode() );

            // headers
            response.headers().map().forEach( (name,values) -> {
                //debug( "Response Header: %s: %s", name, values );
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
