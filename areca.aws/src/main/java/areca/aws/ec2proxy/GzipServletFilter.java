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
package areca.aws.ec2proxy;

import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

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

import org.apache.commons.lang3.StringUtils;

import areca.aws.XLogger;

/**
 *
 * @author Falko Bräutigam
 */
public class GzipServletFilter
        implements Filter {

    private static final XLogger LOG = XLogger.get( GzipServletFilter.class );

    public static final List<String> COMPRESSIBLE = Arrays.asList(
            "*/*", "text/html", "text/xml", "text/plain", "text/css", "text/javascript",
            "application/x-javascript", "application/javascript", "application/json", "application/xml" );


    @Override
    public void init( FilterConfig filterConfig ) throws ServletException { }

    @Override
    public void destroy() { }


    @Override
    public void doFilter( ServletRequest request, ServletResponse response, FilterChain chain )
            throws IOException, ServletException {

        var req = (HttpServletRequest)request;
        var resp = (HttpServletResponse)response;

        if (StringUtils.contains( req.getHeader( "Accept-Encoding" ), "gzip" )
                && COMPRESSIBLE.stream().anyMatch( mime -> req.getHeader( "Accept" ).contains( mime ) )) {
            LOG.info( "GZIP: %s ? %s", req.getPathInfo(), req.getQueryString() );
            resp.setHeader( "Content-Encoding", "gzip" );
            chain.doFilter( request, new GzipResponseWrapper( resp ) );
        }
        else {
            chain.doFilter( request, response );
        }
    }

    /**
     *
     */
    protected class GzipResponseWrapper
            extends HttpServletResponseWrapper {

        private ServletOutputStream out;
        private OutputStream zip;
        private int c;

        public GzipResponseWrapper( HttpServletResponse response ) throws IOException {
            super( response );
            out = response.getOutputStream();
            zip = new GZIPOutputStream( response.getOutputStream(), true );
        }

        @Override
        public void setHeader( String name, String value ) {
            throw new RuntimeException( "..." );
        }

        @Override
        public void addHeader( String name, String value ) {
            if (name.equalsIgnoreCase( "Content-Encoding" )) {
                // already gzipped
                if (value.equalsIgnoreCase( "gzip" )) {
                    LOG.info( "Already: %s : %s", name, value );
                    super.resetBuffer();
                    zip = out;
                    super.setHeader( name, value );
                }
                else {
                    LOG.info( "Preventing header: %s : %s", name, value );
                }
            }
            else if (name.equalsIgnoreCase( "Content-Length" )) {
                super.addHeader( "X-Uncompressed-Content-Length", value );
            }
            else {
                super.addHeader( name, value );
            }
        }

        @Override
        public void flushBuffer() throws IOException {
            zip.close();
            getResponse().flushBuffer();
            LOG.info( "FLUSH BUFFER: %s", c );
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            return new ServletOutputStream() {
                @Override
                public void write( int b ) throws IOException {
                    zip.write( b );
                    c += 1;
                }
                @Override
                public void write( byte[] b ) throws IOException {
                    zip.write( b );
                    c += b.length;
                }
                @Override
                public void write( byte[] b, int off, int len ) throws IOException {
                    zip.write( b, off, len );
                    c += len;
                }
                @Override
                public void setWriteListener( WriteListener writeListener ) {
                    out.setWriteListener( writeListener );
                }
                @Override
                public boolean isReady() {
                    return out.isReady();
                }
                @Override
                public void flush() throws IOException {
                    zip.flush();
                }
                @Override
                public void close() throws IOException {
                    zip.flush();
                    zip.close();
                    getResponse().flushBuffer();
                    LOG.info( "CLOSED (%s bytes)", c );
                }
            };
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            throw new RuntimeException( "not yet implemented." );
        }
    }

}
