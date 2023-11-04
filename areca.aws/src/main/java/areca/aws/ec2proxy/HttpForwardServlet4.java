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

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Timer;

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
import areca.aws.XLogger;
import areca.aws.ec2proxy.EnsureEc2InstanceHandler.Mode;
import areca.aws.logs.EventCollector;
import areca.aws.logs.GsonEventTransformer;
import areca.aws.logs.HttpRequestEvent;
import areca.aws.logs.OpenSearchSink;

/**
 *
 * https://stackoverflow.com/questions/2793150/how-to-use-java-net-urlconnection-to-fire-and-handle-http-requests
 *
 * @author Falko Bräutigam
 */
public class HttpForwardServlet4
        extends HttpServlet {

    private static final XLogger LOG = XLogger.get( HttpForwardServlet4.class );

    public static final List<String> METHODS_WITH_BODY = Arrays.asList("POST", "REPORT", "PUT");

    public static final List<String> FORBIDDEN_HEADERS = Arrays.asList( "host", "connection", "keep-alive",
            "proxy-authenticate", "proxy-authorization", "te", "trailers", "transfer-encoding", "upgrade",
            "content-length" );

    public static final Duration TIMEOUT_CONNECT = Duration.ofSeconds( 3 );
    public static final Duration TIMEOUT_REQUEST = Duration.ofSeconds( 10 );
    public static final Duration TIMEOUT_SERVICES_STARTUP = Duration.ofSeconds( 60 );

    public static final String LOG_REQUEST = "requests";
    public static final String LOG_INSTANCE = "instance-status";

    // instance *******************************************

    public static Timer     timer;

    public static EventCollector<Object,String> logs;

    private List<VHost>     vhosts;

    private HttpClient      http;

    private AWS             aws;


    @Override
    public void init() throws ServletException {
        log( getClass().getSimpleName() + " init..." );
        try {
            var noCookies = new CookieHandler() {
                @Override
                public Map<String,List<String>> get( URI uri, Map<String,List<String>> requestHeaders ) throws IOException {
                    return Collections.emptyMap();
                }
                @Override
                public void put( URI uri, Map<String,List<String>> responseHeaders ) throws IOException {
                    //LOG.debug( "############## cookie: %s", uri );
                    responseHeaders.entrySet().stream()
                            .filter( entry -> entry.getKey().startsWith( "set-cookie" ) )
                            .forEach( entry -> LOG.info( "    %s : %s", entry.getKey(), entry.getValue() ) );
                }
            };
            System.setProperty( "jdk.httpclient.allowRestrictedHeaders", "host" );
            http = HttpClient.newBuilder()
                    .connectTimeout( TIMEOUT_CONNECT )
                    .cookieHandler( noCookies )
                    //.cookieHandler( new CookieManager( null, CookiePolicy.ACCEPT_ALL ) )
                    .build();

            aws = new AWS();

            timer = new Timer();

            vhosts = VHost.readConfig( aws );
            vhosts.forEach( vhost -> vhost.init( aws ) );

            logs = new EventCollector<Object,String>()
                    .addTransform( new GsonEventTransformer<Object>() )
                    .addSink( new OpenSearchSink( null, null ) );
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new ServletException( e );
        }
    }


    @Override
    public void destroy() {
        log( getClass().getSimpleName() + ": destroy ..." );
        vhosts.forEach( vhost -> vhost.dispose() );
        aws.dispose();
        timer.cancel();
        timer = null;
        logs.dispose();
    }


    @Override
    protected void service( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException {
        var ev = HttpRequestEvent.prepare( req, resp );
        LOG.info( "%s %s ?%s", req.getMethod(), ev.url, defaultIfEmpty( req.getQueryString(), "-" ) );
        //LOG.info( "Path:'%s'", req.getPathInfo() );

        try {
            doService( req, resp, ev );
        }
        catch (SignalErrorResponseException e) {
            ev.error = e.toString();
            LOG.info( "Response: %s - %s", e.status, e.getMessage() );
            resp.sendError( e.status, e.getMessage() );
        }
        catch (Exception e) {
            ev.error = e.toString();
            e.printStackTrace();
            resp.sendError( 500, e.getMessage() );
        }
        finally {
            logs.publish( LOG_REQUEST, ev.complete() );
        }
    }


    protected void doService( HttpServletRequest req, HttpServletResponse resp, HttpRequestEvent event ) throws Exception {
        var probe = new RequestHandler.Probe( req, resp );
        probe.aws = aws;
        probe.http = http;

        // vhost
        probe.vhost = vhosts.stream()
                .filter( _vhost -> _vhost.hostnames.contains( req.getServerName() ) ).findAny()
                .orElseThrow( () -> new SignalErrorResponseException( 404, "No such server: " + req.getServerName() ) )
                .touch();

        // proxypath
        probe.proxyPath = probe.vhost.proxypaths.stream()
                .filter( path -> req.getPathInfo().startsWith( path.path ) ).findAny()
                .orElseThrow( () -> new SignalErrorResponseException( 404, "No such path: " + req.getPathInfo() ) );

        probe.redirect = probe.proxyPath.forward
                + req.getPathInfo().substring( probe.proxyPath.path.length() )
                + (req.getQueryString() != null ? "?"+req.getQueryString() : "" );

        LOG.info( "    -> %s", probe.redirect );
        event.vhost = probe.vhost.hostnames.get( 0 );
        event.forward = probe.redirect;

        // handle
        var requestHandlers = Arrays.asList(
                new IndexRedirectHandler(),
                new EnsureEc2InstanceHandler( Mode.LOADING_PAGE ),
                new StraightForwardHandler(),
                new EnsureEc2InstanceHandler.AfterError( Mode.LOADING_PAGE ),
                new SanityCheckHandler() );

        for (var handler : requestHandlers) {
            if (handler.canHandle( probe )) {
                try {
                    LOG.info( " ============ %s ============", handler.getClass().getSimpleName() );
                    handler.handle( probe );
                }
                catch (Exception e) {
                    LOG.info( "    : %s", e );
                    e.printStackTrace();
                    probe.error = e;
                }
            }
        }
    }
}
