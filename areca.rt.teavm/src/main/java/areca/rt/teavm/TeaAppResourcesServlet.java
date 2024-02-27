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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Level;
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


    @Override
    public void init() throws ServletException {
        LogFactory.setClassLevel( TeaAppResourcesServlet.class, Level.DEBUG );
    }


    @Override
    protected void doGet( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException {
        String path = req.getPathInfo();
        LOG.debug( "Path: %s", path );

        if (path == null) {
            resp.sendRedirect( "index.html" );
        }
        if (path.equals( "/" )) {
            path = "index.html";
        }

//        LOG.info( "res: ServletContext" );
//        for (String s : getServletContext().getResourcePaths( "/" )) {
//            LOG.info( "   %s : %s", s, getServletContext().getResourceAsStream( s ) );
//        }


        var in = Thread.currentThread().getContextClassLoader().getResourceAsStream( path );
        if (in != null) {
            LOG.debug( "  -> classpath: %s", path );
        }
        else {
            in = getServletContext().getResourceAsStream( path );
            if (in != null) {
                LOG.debug( "  -> context: %s", path );
            }
            else {
                LOG.warn( "No resource: %s", path );
                resp.setStatus( 404 );
                return;
            }
        }

        try (
            var out = resp.getOutputStream();
        ){
            var buf = new byte[4096];
            for (var c = in.read( buf ); c > -1; c = in.read( buf )) {
                out.write( buf, 0, c );
            }
        }
        finally {
            in.close();
        }
    }

}
