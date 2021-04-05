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
package areca.app.service.imap;

import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;

import java.util.Date;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingDeque;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import areca.app.service.imap.ImapRequestData.CommandData;
import areca.common.Timer;

/**
 *
 * https://tewarid.github.io/2011/05/10/access-imap-server-from-the-command-line-using-openssl.html
 *
 * @author Falko Bräutigam
 */
//@WebServlet(name = "ImapForwardServlet", urlPatterns = {"/imap"})
public class ImapForwardServlet extends HttpServlet {

    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    static class InOut {
        public PrintWriter      out;
        public BufferedReader   in;
    }

    @FunctionalInterface
    interface Consumer<T,E extends Exception> {
        void accept( T consumable ) throws E;
    }


    // instance *******************************************

    private SocketPool     socketPool = new SocketPool();


    @Override
    public void init() throws ServletException {
        log( "" + getClass().getSimpleName() + " init..." );
    }


    @Override
    public void doPost( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
        debug( "\n\nREQUEST: length=" + request.getContentLength() );
        var timer = Timer.start();
        var imapRequest = gson.fromJson( request.getReader(), ImapRequestData.class );
        var out = response.getWriter();

        try (
            var ssl = createSSLSocket( imapRequest );
            var sslOut = new PrintWriter( new BufferedWriter( new OutputStreamWriter( ssl.getOutputStream(), "UTF8" ) ) );
            var sslIn = new BufferedReader( new InputStreamReader( ssl.getInputStream(), "UTF8" ) );
        ){
            debug( "####################################### SSL connection: established. (" + timer.elapsedHumanReadable() + ")" );
            timer.restart();

            var imap = new InOut() {{out = sslOut; in = sslIn;}};
            debug( "<< " + imap.in.readLine() );
            debug( "####################################### First line: read. (" + timer.elapsedHumanReadable() + ")" );
            timer.restart();

            performCommand( imapRequest.loginCommand, imap, null );

            for (var command : imapRequest.commands) {
                performCommand( command, imap, line -> {
                    out.write( line );
                    out.write( "\n" );
                });
                // out.write( ImapRequest.RESPONSE_COMMAND_DELIMITER );
                out.flush();
            }

            performCommand( new ImapRequest.LogoutCommand(), imap, null );
        }
        catch (/*KeyManagementException | NoSuchAlgorithmException |*/ Exception e) {
            throw new ServletException( e );
        }
        debug( "SSL connection closed." );
    }


    protected <E extends Exception> void performCommand( CommandData command, InOut imap, Consumer<String,E> sink ) throws E, IOException {
        Timer timer = Timer.start();
        debug( "> " + command.command );
        imap.out.println( command.command );
        imap.out.flush();

        if (imap.out.checkError()) {
            throw new IOException( "SSLSocketClient: java.io.PrintWriter error" );
        }

        for (String line = imap.in.readLine();; line = imap.in.readLine()) {
            debug( "< " + line );
            if (sink != null) {
                sink.accept( line );
            }
            if (containsIgnoreCase( line, command.expected )) {
                break;
            }
        }
        debug( "####################################### Command: done. (" + timer.elapsedHumanReadable() + ")\n" );
    }


    protected SSLSocket createSSLSocket( ImapRequestData imapRequest )
            throws IOException, NoSuchAlgorithmException, KeyManagementException {
        SSLContext sslContext = SSLContext.getInstance( "TLS" );
        TrustManager tm = new X509TrustManager() {
            @Override
            public void checkClientTrusted( X509Certificate[] chain, String authType ) throws CertificateException {
            }
            @Override
            public void checkServerTrusted( X509Certificate[] chain, String authType ) throws CertificateException {
            }
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };
        sslContext.init( null, new TrustManager[] { tm }, null );

        debug( "trying %s:%s ...", imapRequest.host, imapRequest.port );
        SSLSocketFactory factory = sslContext.getSocketFactory();
        SSLSocket socket = (SSLSocket)factory.createSocket( imapRequest.host, imapRequest.port );
        socket.startHandshake();
        return socket;
    }


    protected void debug( String msg, Object... args ) {
        System.out.println( String.format( msg, args ) );
    }


    /**
     *
     */
    class SocketPool {

        private ConcurrentMap<String,BlockingDeque<PooledSocket>> pool = new ConcurrentHashMap<>();


        protected String key( ImapRequestData request ) {
            return String.join( "_", request.host, String.valueOf( request.port ), request.loginCommand.command.toString() );
        }

        public PooledSocket aquireSocket( ImapRequest request ) throws Exception {
            var sockets = pool.computeIfAbsent( key( request ), k -> new LinkedBlockingDeque<>() );
            var pooled = sockets.poll();
            if (pooled != null) {
                return pooled;
            }
            else {
                return new PooledSocket( createSSLSocket( request ) );
            }
        }

        public void release( PooledSocket socket ) {
            // var sockets = pool.get( key( request ) );
        }

        class PooledSocket {

            private Socket  socket;

            private Date    lastUsed;

            public PooledSocket( Socket socket ) {
                this.socket = socket;
            }

            public Socket use() {
                lastUsed = new Date();
                return socket;
            }
        }
    }


//    public static void main(String[] args) throws Exception {
//        Gson gson = new GsonBuilder().setPrettyPrinting().create();
//        ImapRequest request = new ImapRequest( self -> {
//            self.host = "host";
//            self.port = 993;
//            self.commands.add( new Command( c -> {
//                c.command = "command";
//            }));
//        });
//        log.info( "Json: " + gson.toJson( request ) );
//    }
}
