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
package areca.rt.teavm;

import static org.apache.commons.lang3.StringUtils.defaultString;

import java.util.Collections;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * Providing resources necessary for running a {@link TeaApp}. The resources are
 * loaded from classpath.
 * <ul>
 * <li>...</li>
 * </ul>
 *
 * @author Falko Bräutigam
 */
public class TeaAppResourcesServlet
        extends HttpServlet {

    private static final Log LOG = LogFactory.getLog( TeaAppResourcesServlet.class );

    private String etag = "\"" + System.currentTimeMillis() + "\"";

    @Override
    public void init() throws ServletException {
        //LogFactory.setClassLevel( TeaAppResourcesServlet.class, Level.DEBUG );
    }

    @Override
    protected void doGet( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException {
        LOG.debug( "PATH: %s", req.getPathInfo() );
        try {
            resp.setHeader( "Etag", etag );

            // check etag -> 304
            var ifNoneMatch = req.getHeader(  "If-None-Match" );
            if (etag.equals( ifNoneMatch )) {
                LOG.debug( "304: %s", req.getPathInfo() );
                resp.setStatus( HttpServletResponse.SC_NOT_MODIFIED );
                resp.flushBuffer();
                return;
            }

            // load resource
            processGet( req, resp );
        }
        catch (Throwable e) {
            resp.setStatus( 500 );
            e.printStackTrace( System.err );
        }
    }

    protected void processGet( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException {
        String path = req.getPathInfo();

        // only happens if the servlet is *not* the default servlet and
        // and incoming URL has no trailing /
        if (path == null) {
            LOG.debug( "  contextPath: %s", req.getContextPath() );
            LOG.debug( "  servletPath: %s", req.getServletPath() );
            var loc = req.getContextPath() + req.getServletPath() + "/" + defaultString( req.getQueryString() );
            log( req, "REDIRECT", loc );
            resp.sendRedirect( loc );
            return;
        }
        //
        if (path.equals( "/" )) {
            path = "/index.html";
        }
        //
        if (!path.contains( "." ) || path.endsWith( ".class" )) {
            LOG.warn( "Not allowed: %s", path );
            resp.setStatus( 404 );
            return;
        }

        // classpath
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        var in = cl.getResourceAsStream( path );
        if (in != null) {
            var l = Collections.list( cl.getResources( path ) );
            if (l.size() > 1) {
                l.forEach( res -> LOG.warn( "MULTIPLE: %s", res ) );
            }
            log( req, "CLASSPATH", "~" );
        }
        // webapp context (war:/WEB_INF/... or jar:/META_INF/resources)
        else {
            in = getServletContext().getResourceAsStream( path );
            if (in != null) {
                log( req, "CONTEXT", "~" );
            }
            else {
                LOG.warn( "No resource: %s", path );
                resp.setStatus( 404 );
                return;
            }
        }

        var buffSize = 32 * 1024;
        resp.setBufferSize( buffSize );
        try (
            var out = resp.getOutputStream();
        ){
            var buf = new byte[buffSize];
            for (var c = in.read( buf ); c > -1; c = in.read( buf )) {
                out.write( buf, 0, c );
            }
        }
        finally {
            in.close();
        }
    }


    protected void log( HttpServletRequest req, String method, String path ) {
        LOG.debug( "        -> %s (%s)", method, path );
    }

}
