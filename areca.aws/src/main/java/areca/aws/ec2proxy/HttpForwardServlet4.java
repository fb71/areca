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
package areca.aws.ec2proxy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import areca.aws.AWS;
import areca.aws.ec2proxy.EnsureEc2InstanceHandler.Mode;

/**
 *
 * https://stackoverflow.com/questions/2793150/how-to-use-java-net-urlconnection-to-fire-and-handle-http-requests
 *
 * @author Falko Bräutigam
 */
public class HttpForwardServlet4
        extends HttpServlet {

    public static final List<String> METHODS_WITH_BODY = Arrays.asList("POST", "REPORT", "PUT");

    public static final List<String> FORBIDDEN_HEADERS = Arrays.asList( "host", "connection", "keep-alive",
            "proxy-authenticate", "proxy-authorization", "te", "trailers", "transfer-encoding", "upgrade",
            "content-length" );

    public static final Duration TIMEOUT_CONNECT = Duration.ofSeconds( 3 );
    public static final Duration TIMEOUT_REQUEST = Duration.ofSeconds( 10 );
    public static final Duration TIMEOUT_SERVICES_STARTUP = Duration.ofSeconds( 60 );


    // instance *******************************************

    private List<VHost>     vhosts;

    private HttpClient      http;

    private AWS             aws;


    @Override
    public void init() throws ServletException {
        log( getClass().getSimpleName() + " init..." );

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

        aws = new AWS();

        vhosts = VHost.readConfig( aws );
        vhosts.forEach( vhost -> vhost.init( aws ) );
    }


    @Override
    public void destroy() {
        log( getClass().getSimpleName() + ": destroy ..." );
        vhosts.forEach( vhost -> vhost.dispose() );
        aws.dispose();
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
            debug( "%s", e.getMessage() );
            resp.sendError( 404, e.getMessage() );
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException( e );
        }
    }


    protected void doService( HttpServletRequest req, HttpServletResponse resp ) throws Exception {
        var start = System.nanoTime();
        debug( "URI: %s %s://%s:%s/%s ?%s", req.getMethod(),
                req.getScheme(), req.getServerName(), req.getServerPort(), req.getRequestURI(), req.getQueryString() );
        debug( "Path:'%s'", req.getPathInfo() );

        var probe = new HttpHandler.Probe();
        probe.aws = aws;
        probe.http = http;
        probe.request = req;
        probe.response = resp;

        // find vhost
        probe.vhost = vhosts.stream()
                .filter( _vhost -> _vhost.hostnames.contains( req.getServerName() ) ).findAny()
                .orElseThrow( () -> new NoSuchElementException( "No vhost found for server: " + req.getServerName() ) )
                .touch();

        // find redirect
        probe.proxyPath = probe.vhost.proxypaths.stream()
                .filter( p -> req.getPathInfo().startsWith( p.path ) ).findAny()
                .orElseThrow( () -> new NoSuchElementException( "No proxypath found for: " + req.getPathInfo() ) );

        probe.redirect = probe.proxyPath.redirect
                + req.getPathInfo().substring( probe.proxyPath.path.length() )
                + (req.getQueryString() != null ? "?"+req.getQueryString() : "" );
        debug( "    -> %s", probe.redirect );
        debug( "    -> indexRedirect: %s", probe.vhost.indexRedirect );

        // handle
        var requestHandlers = Arrays.asList(
                //new IndexRedirectHandler(),
                new EnsureEc2InstanceHandler( Mode.WAIT ),
                new StraightForwardHandler(),
                new EnsureEc2InstanceHandler.AfterError( Mode.WAIT ),
                new SanityCheckHandler() );

        for (var handler : requestHandlers) {
            if (handler.canHandle( probe )) {
                try {
                    debug( " ============ %s ============", handler.getClass().getSimpleName() );
                    handler.handle( probe );
                }
                catch (Exception e) {
                    debug( "    : %s", e );
                    probe.error = e;
                }
            }
        }
        debug( "    -> time: %s ms", (System.nanoTime() - start)/1000000 );
    }


    protected static void debug( String msg, Object... args ) {
        System.out.println( args.length == 0 ? msg : String.format( msg, args ) );
    }

}
