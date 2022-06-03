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
package areca.app.service.http;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Base64;
import java.util.Objects;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URI;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * https://stackoverflow.com/questions/2793150/how-to-use-java-net-urlconnection-to-fire-and-handle-http-requests
 *
 * @author Falko Bräutigam
 */
public class HttpForwardServlet2 extends HttpServlet {

    private SSLContext      sslContext;

    @Override
    public void init() throws ServletException {
        log( "" + getClass().getSimpleName() + " init..." );
        sslContext = SSLUtils.trustAllSSLContext();
        HttpsURLConnection.setDefaultSSLSocketFactory( sslContext.getSocketFactory() );
        HttpsURLConnection.setDefaultHostnameVerifier( SSLUtils.DO_NOT_VERIFY );

        // Arrays.stream( Security.getProviders() ).forEach( p -> debug( "Provider: " + p.getInfo() ) );
    }

    int c = 0; // ich bin müde :)

    @Override
    protected void service( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException {
        try {
            var uri = new StringBuilder( req.getParameter( "uri" ) );

            c = 0;
            req.getParameterMap().forEach( (key,values) -> {
                if (!key.equals( "uri" )) {
                    uri.append( c++==0 ? "?" : "&").append( String.format( "%s=%s", key, values[0] ) );
                }
            });
            debug( "URI: %s %s", req.getMethod(), uri );

            // connection
            HttpURLConnection conn = (HttpURLConnection)URI.create( uri.toString() ).toURL().openConnection();

            // authentication
            if (req.getHeader( "X_Auth_Username" ) != null) {
                String auth = req.getHeader( "X_Auth_Username" ) + ":" + req.getHeader( "X_Auth_Password" );
                byte[] encodedAuth = Base64.getEncoder().encode( auth.getBytes( UTF_8 ) );
                String basic = "Basic " + new String( encodedAuth );
                conn.setRequestProperty( "Authorization", basic );
                debug( "Authorization: %s", basic );
            }

            // headers
            req.getHeaderNames().asIterator().forEachRemaining( name -> {
                if (!name.startsWith( "X" )) {
                    conn.setRequestProperty( name, req.getHeader( name ) );
                    debug( "Request Header: %s: %s", name, req.getHeader( name ) );
                }
            });

            // method -> send
            setRequestMethod( conn, req.getMethod() );
            if ("POST".equals( req.getMethod() ) || "REPORT".equals( req.getMethod() )) {
                conn.setDoOutput( true );
                setRequestMethod( conn, req.getMethod() );
                copyAndClose( req.getInputStream(), conn.getOutputStream() );
            }

            resp.setStatus( conn.getResponseCode() );
            conn.getHeaderFields().forEach( (name,values) -> {
                debug( "Response Header: %s: %s", name, values );
                if (name != null) {
                    resp.addHeader( name, values.get( 0 ) ); // FIXME
                }
            });
            if (conn.getResponseCode() < 299) {
                copyAndClose( conn.getInputStream(), resp.getOutputStream() );
            }
            else {
                copyAndClose( conn.getErrorStream(), resp.getOutputStream() );
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
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


    protected void setRequestMethod( HttpURLConnection conn, String method ) {
        try {

            if (conn.getClass().getName().equals( "sun.net.www.protocol.https.HttpsURLConnectionImpl" )) {
                Field f = conn.getClass().getDeclaredField( "delegate" );
                f.setAccessible( true );
                conn = (HttpURLConnection)f.get( conn );
            }
            Field f = HttpURLConnection.class.getDeclaredField( "method" );
            f.setAccessible( true );
            f.set( conn, method );

            assert Objects.equals( method, f.get( conn ) ); //, "reflection" );
            assert Objects.equals( method, conn.getRequestMethod() ); //, "getRequestMethod()" );

//            final Class<?> connClass = conn.getClass();
//            final Class<?> parentClass = connClass.getSuperclass();
//            final Field field;
//            // If the implementation class is an HTTPS URL Connection, we
//            // need to go up one level higher in the heirarchy to modify the
//            // 'method' field.
//            if (parentClass == HttpsURLConnection.class) {
//                field = parentClass.getSuperclass().getDeclaredField( "method" );
//            } else {
//                field = parentClass.getDeclaredField( "method" );
//            }
//            field.setAccessible( true );
//            field.set( conn, method);
        }
        catch (final Exception e) {
            throw new RuntimeException( e );
        }
    }


    protected void debug( String msg, Object... args ) {
        System.out.println( args.length == 0 ? msg : String.format( msg, args ) );
    }

}
