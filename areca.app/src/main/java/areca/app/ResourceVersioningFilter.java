/*
 * Copyright (C) 2022, the @authors. All rights reserved.
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
package areca.app;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 *
 * @author Falko Bräutigam
 */
public class ResourceVersioningFilter
        implements Filter {

    @Override
    public void init( FilterConfig filterConfig ) throws ServletException {
    }


    @Override
    public void doFilter( ServletRequest request, ServletResponse response, FilterChain chain )
            throws IOException, ServletException {

        var http = (HttpServletRequest)request;
        System.out.println( "FILTER: " + http.getRequestURI() + " - " + http.getQueryString() );

        var version = http.getParameter( "v" );
        if (http.getRequestURI().endsWith( "/" ) || http.getRequestURI().endsWith( "/index.html" )) {
            var captured = new CapturingResponseWrapper( (HttpServletResponse)response );

            chain.doFilter( request, captured );

            version = version!= null ? version : "";
            var filtered = new String( captured.out.toByteArray(), "UTF-8" ).replace( "%version%", version );
            response.getOutputStream().write( filtered.getBytes( "UTF-8" ) );
        }
        else {
            chain.doFilter( request, response );
        }
    }


    @Override
    public void destroy() {
    }


    protected class CapturingResponseWrapper
            extends HttpServletResponseWrapper {

        public ByteArrayOutputStream out = new ByteArrayOutputStream( 4096 );

        public CapturingResponseWrapper( HttpServletResponse response ) {
            super( response );
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            return new ServletOutputStream() {
                @Override
                public void write( int b ) throws IOException {
                    out.write( b );
                }
                @Override
                public void setWriteListener( WriteListener writeListener ) {
                    throw new RuntimeException( "not yet implemented." );
                }
                @Override
                public boolean isReady() {
                    throw new RuntimeException( "not yet implemented." );
                }
            };
        }

    }
}
