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
package areca.rt.server.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * Providing resources from WEB_INF/lib jars.
 *
 * @author Falko Bräutigam
 */
public class ResourceServlet
        extends HttpServlet {

    private static final Log LOG = LogFactory.getLog( ResourceServlet.class );

    @Override
    protected void doGet( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException {
        String res = req.getPathInfo();
        LOG.info( "PATH: %s -> %s", req.getPathInfo(), res );

//        LOG.info( "res: ServletContext" );
//        for (String s : getServletContext().getResourcePaths( "/" )) {
//            LOG.info( "   %s : %s", s, getServletContext().getResourceAsStream( s ) );
//        }

        try (
            var in = getServletContext().getResourceAsStream( res );
            //var in = Thread.currentThread().getContextClassLoader().getResourceAsStream( res );
            //var in = ResourceServlet.class.getClassLoader().getResourceAsStream( res );
            var out = resp.getOutputStream();
        ){
            var buf = new byte[4096];
            for (var c = in.read( buf ); c > -1; c = in.read( buf )) {
                out.write( buf, 0, c );
            }
        }
    }

}
