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

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.rightPad;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import java.io.IOException;
import java.io.InputStream;
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
import areca.aws.logs.LogRequestHandler;
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
            "content-length", "expect" );

    public static final Duration TIMEOUT_CONNECT = Duration.ofSeconds( 3 );
    public static final Duration TIMEOUT_REQUEST = Duration.ofSeconds( 10 );
    public static final Duration TIMEOUT_SERVICES_STARTUP = Duration.ofSeconds( 60 );

    public static final String LOG_REQUEST = "requests";
    public static final String LOG_INSTANCE = "instance-status";

    public static final int BUFFER_SIZE = 16 * 1024;

    /**
     * {@link ClassLoader#getResourceAsStream(String)}
     */
    public static InputStream resourceAsStream( String name ) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream( name );
    }

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
                            .forEach( entry -> LOG.debug( "    %s : %s", entry.getKey(), entry.getValue() ) );
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
        try {
            doService( req, resp );
        }
        catch (SignalErrorResponseException e) {
            resp.sendError( e.status, e.getClass().getSimpleName() + ": " + e.getMessage() );
        }
        catch (Exception e) {
            e.printStackTrace();
            resp.sendError( 500, e.getClass().getSimpleName() + ": " + e.getMessage() );
        }
    }


    protected void doService( HttpServletRequest req, HttpServletResponse resp ) throws Exception {
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

        // handle
        var requestHandlers = Arrays.asList(
                new GzipServletFilter(),
                new SimpleErrorPageHandler(),
                new LogRequestHandler(),
                new RobotsSitemapHandler(),
                new FuckOffHandler(),
                new IndexRedirectHandler(),
                new PreventRobotStartupHandler(),
                new EnsureEc2InstanceHandler( Mode.LOADING_PAGE ),
                new StraightForwardHandler(),
                new EnsureEc2InstanceHandler.AfterError( Mode.LOADING_PAGE ),
                new SanityCheckHandler() );

        var start = System.nanoTime();
        for (var handler : requestHandlers) {
            if (handler.canHandle( probe )) {
                try {
                    LOG.info( rightPad( format( "====== %s ", handler.getClass().getSimpleName() ), 60, "=" ) );
                    handler.handle( probe );
                }
                catch (Exception e) {
                    LOG.info( "    : %s", e );
                    e.printStackTrace();
                    probe.error = e;
                }
            }
        }
        LOG.warn( "*** %s (%s ms)", probe.request.getPathInfo(), (System.nanoTime()-start)/1000000 );
        //LOG.info( "------------------------------------------------------------" );
        LOG.info( "____________________________________________________________" );
    }
}
