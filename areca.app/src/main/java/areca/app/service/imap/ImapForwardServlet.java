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

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingDeque;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import areca.app.service.http.SSLUtils;
import areca.app.service.imap.ImapForwardServlet.SocketPool.PooledSocket;
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

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static final Duration POOL_DURATION = Duration.ofSeconds( 3 );

    @FunctionalInterface
    interface Consumer<T,E extends Exception> {
        void accept( T consumable ) throws E;
    }


    // instance *******************************************

    private SocketPool          socketPool = new SocketPool();

    private SSLContext          sslContext;


    @Override
    public void init() throws ServletException {
        log( "" + getClass().getSimpleName() + " init..." );
        sslContext = SSLUtils.trustAllSSLContext();
    }


    @Override
    public void destroy() {
        socketPool.dispose();
        super.destroy();
    }


    @Override
    public void doPost( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
        debug( "\n\nREQUEST: length=" + request.getContentLength() );
        var timer = Timer.start();
        var imapRequest = gson.fromJson( request.getReader(), ImapRequestData.class );
        var out = response.getWriter();

        var imap = socketPool.aquireSocket( imapRequest );
        try {
            imap.ifCreated( s -> {
                debug( "<< " + imap.in.readLine() );
                debug( "####################################### First line: read. (" + timer.elapsedHumanReadable() + ")" );
                performCommand( imapRequest.loginCommand, imap, null );
            });

            for (var command : imapRequest.commands) {
                performCommand( command, imap, line -> {
                    out.write( line );
                    out.write( '\n' );
                });
                out.flush();
            }
            socketPool.release( imap );
        }
        catch (Exception e) {
            imap.close();
            throw new ServletException( e );
        }
    }


    protected <E extends Exception> void performCommand( CommandData command, PooledSocket imap, Consumer<String,E> sink ) throws E, IOException {
        Timer timer = Timer.start();
        debug( "> " + command.command );
        imap.out.write( command.command );
        imap.out.write( "\r\n" );
        imap.out.flush();

        for (String line = imap.in.readLine();; line = imap.in.readLine()) {
            // debug( "< " + line );
            if (sink != null) {
                sink.accept( line );
            }
            if (containsIgnoreCase( line, command.expected )) {
                break;
            }
        }
        debug( "####################################### Command: done. (" + timer.elapsedHumanReadable() + ")\n" );
    }


    protected SSLSocket createSSLSocket( ImapRequestData imapRequest ) throws IOException {
        Timer timer = Timer.start();
        debug( "trying %s:%s ...", imapRequest.host, imapRequest.port );
        SSLSocketFactory factory = sslContext.getSocketFactory();
        SSLSocket socket = (SSLSocket)factory.createSocket( imapRequest.host, imapRequest.port );
        socket.startHandshake();
        debug( "####################################### SSL connection: established. (" + timer.elapsedHumanReadable() + ")" );
        return socket;
    }


    protected void debug( String msg, Object... args ) {
        System.out.println( args.length == 0 ? msg : String.format( msg, args ) );
    }


    /**
     *
     */
    class SocketPool {

        private ConcurrentMap<String,BlockingDeque<PooledSocket>> pool = new ConcurrentHashMap<>();

        private Thread  scavenger;

        protected SocketPool() {
            scavenger = new Thread( () -> {
                while (scavenger != null) {
                    try {
                        Thread.sleep( 1000 );

                        var threshold = Instant.now().minus( POOL_DURATION );
                        for (var poolkey : pool.keySet()) {
                            var newDeque = new LinkedBlockingDeque<PooledSocket>();
                            var sockets = pool.put( poolkey, newDeque );
                            for (var pooled : sockets) {
                                if (pooled.lastUsed.isBefore( threshold ) || scavenger == null) {
                                    pooled.close();
                                }
                                else {
                                    newDeque.addFirst( pooled );
                                }
                            }
                        }
                    }
                    catch (Exception e) {
                        debug( "%s", e.getMessage() );
                        e.printStackTrace();
                    }
                }
                debug( "POOL: stopped." );
            }, "SocketPool Scavenger"  );
            scavenger.setDaemon( true );
            scavenger.start();
        }


        protected void dispose() {
            this.scavenger = null;
        }


        public PooledSocket aquireSocket( ImapRequestData request ) throws IOException {
            String poolkey = String.join( "|", request.host, String.valueOf( request.port ), request.loginCommand.command );
            var sockets = pool.computeIfAbsent( poolkey, k -> new LinkedBlockingDeque<>() );
            var pooled = sockets.poll();
            if (pooled != null) {
                debug( "POOL: aquired POOLED (pool size: %d/%d)", pool.size(), sockets.size() );
                return pooled;
            }
            else {
                debug( "POOL: aquired NEW (pool size: %d/%d)", pool.size(), sockets.size() );
                return new PooledSocket( createSSLSocket( request ), poolkey );
            }
        }


        public void release( PooledSocket pooled ) {
            pool.get( pooled.poolkey).addLast( pooled.touch() );
            debug( "POOL: pooled socket released (pool size: %d/%d)", pool.size(), pool.get( pooled.poolkey ).size() );
        }


        class PooledSocket {

            private String          poolkey;

            private Socket          socket;

            private BufferedWriter  out;

            private BufferedReader  in;

            private Instant         lastUsed;

            public PooledSocket( Socket socket, String poolkey ) throws IOException {
                this.poolkey = poolkey;
                this.socket = socket;
                this.out = new BufferedWriter( new OutputStreamWriter( socket.getOutputStream(), "UTF8" ) );
                this.in = new BufferedReader( new InputStreamReader( socket.getInputStream(), "UTF8" ) );
            }

            public <E extends Exception> void ifCreated( Consumer<Socket,E> block ) throws E {
                if (lastUsed == null) {
                    block.accept( socket );
                }
            }

            public PooledSocket touch() {
                lastUsed = Instant.now();
                return this;
            }

            public void close() {
                try {
                    // performCommand( new ImapRequest.LogoutCommand(), imap, null );
                    socket.close();
                    socket = null;
                    in = null;
                    out = null;
                }
                catch (IOException e) {
                    debug( "ERROR: while closing socket: %s", e.getMessage() );
                }
                debug( "POOL: socket closed." );
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
