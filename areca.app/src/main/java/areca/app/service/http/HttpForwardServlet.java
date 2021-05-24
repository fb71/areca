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

import java.util.Arrays;

import java.io.IOException;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.Security;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 *
 * @author Falko Bräutigam
 */
public class HttpForwardServlet extends HttpServlet {

    private SSLContext      sslContext;

    private HttpClient      httpClient;

    @Override
    public void init() throws ServletException {
        log( "" + getClass().getSimpleName() + " init..." );
        sslContext = SSLUtils.trustAllSSLContext();
        HttpsURLConnection.setDefaultSSLSocketFactory( sslContext.getSocketFactory() );
        HttpsURLConnection.setDefaultHostnameVerifier( new HostnameVerifier() {
            @Override
            public boolean verify( String hostname, SSLSession session ) {
                return true;
            }
        } );

        httpClient = HttpClient.newBuilder()
                .sslContext( sslContext )
                .authenticator( new Authenticator() {
                    @Override
                    public PasswordAuthentication requestPasswordAuthenticationInstance( String host, InetAddress addr,
                            int port, String protocol, String prompt, String scheme, URL url, RequestorType reqType ) {
                        debug( "Auth: " + host );
                        return null;
                    }
                })
                .build();

        Arrays.stream( Security.getProviders() ).forEach( p -> debug( "Provider: " + p.getInfo() ) );
    }


    @Override
    protected void service( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException {
        try {
            var uri = req.getParameter( "uri" );
            debug( "URI: %s", uri );

            var reqIn = req.getInputStream();
            var rb = HttpRequest.newBuilder( URI.create( uri ) )
                    .method( req.getMethod(), BodyPublishers.ofInputStream( () -> reqIn ));

            req.getHeaderNames().asIterator().forEachRemaining( name -> {
                debug( "Header: %s: %s", name, req.getHeader( name ) );
                // rb.setHeader( name, req.getHeader( name ) );
            });

            var clientResponse = httpClient.send( rb.build(), BodyHandlers.ofInputStream() );
            resp.setStatus( clientResponse.statusCode() );
            //resp.setContentType( clientResponse. );
            clientResponse.headers().map().forEach( (n,v) -> resp.addHeader( n, v.get( 0 ) ) ); // FIXME
            try (
                var clientIn = clientResponse.body();
                var out = resp.getOutputStream();
            ) {
                var buf = new byte[4096];
                for (int c = clientIn.read( buf ); c != -1; c = clientIn.read( buf )) {
                    out.write( buf, 0, c );
                }
                out.flush();
            }
        }
        catch (InterruptedException e) {
            throw new RuntimeException( e );
        }
    }


//    public HttpClient getNewHttpClient() {
//        try {
//            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
//            trustStore.load(null, null);
//
//            MySSLSocketFactory sf = new MySSLSocketFactory(trustStore);
//            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
//
//            HttpParams params = new BasicHttpParams();
//            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
//            HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
//
//            SchemeRegistry registry = new SchemeRegistry();
//            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
//            registry.register(new Scheme("https", sf, 443));
//
//            ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);
//
//            return new DefaultHttpClient(ccm, params);
//        } catch (Exception e) {
//            return new DefaultHttpClient();
//        }
//    }



    protected void debug( String msg, Object... args ) {
        System.out.println( args.length == 0 ? msg : String.format( msg, args ) );
    }

}
