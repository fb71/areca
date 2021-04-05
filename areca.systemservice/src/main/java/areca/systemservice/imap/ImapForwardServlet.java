/*
 * Copyright (C) 2021, the @authors. All rights reserved.
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
package areca.systemservice.imap;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * https://tewarid.github.io/2011/05/10/access-imap-server-from-the-command-line-using-openssl.html
 *
 * @author Falko Bräutigam
 */
@WebServlet(
        name = "ImapServlet",
        urlPatterns = {"/imap"}
)
public class ImapForwardServlet extends HttpServlet {

    private static final Log log = LogFactory.getLog( ImapForwardServlet.class );

    @Override
    public void init() throws ServletException {
        log.info( "" + getClass().getSimpleName() + " init..." );
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter  out = response.getWriter();
        out.println("<html>Hello, I am a Java servlet!</html>");
        out.flush();
    }

    @Override
    public void doPost( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException {
        throw new RuntimeException( "not yet implemented." );
    }
}
