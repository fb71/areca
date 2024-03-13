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

import static java.lang.Integer.parseInt;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.LinkedList;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import areca.common.Assert;
import areca.common.MutableInt;
import areca.common.Session;
import areca.common.SessionScoper;
import areca.common.SessionScoper.ThreadBoundSessionScoper;
import areca.common.Timer;
import areca.common.base.Opt;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.rt.server.EventLoop;
import areca.rt.server.ServerApp;
import areca.ui.Position;
import areca.ui.Size;
import areca.ui.component2.ColorPicker;
import areca.ui.component2.Events.EventType;
import areca.ui.component2.Events.UIEvent;
import areca.ui.component2.FileUpload;
import areca.ui.component2.TextField;
import areca.ui.component2.UIComponent;

/**
 *
 * @author Falko Bräutigam
 */
public class ArecaUIServer
        extends HttpServlet {

    private static final String ATTR_SESSION = "areca.session";
    private static final String ATTR_UPLOADED = "areca.uploaded";

    private static final Log LOG = LogFactory.getLog( ArecaUIServer.class );

    /** The HTTP request of (accessible in) the current EventLoop */
    public static ThreadLocal<Request> currentRequest = new ThreadLocal<>();

    private static ThreadBoundSessionScoper sessionScope = new ThreadBoundSessionScoper();

    private Class<ServerApp> appClass;

    private Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private Gson notPretty = new GsonBuilder().create();


    @Override
    @SuppressWarnings({"unchecked", "deprecation"})
    public void init() throws ServletException {
        try {
            // servlet needs <load-on-startup>1</load-on-startup> if other servlets
            SessionScoper.setInstance( sessionScope );
            LOG.warn( "Session scope: %s", ThreadBoundSessionScoper.instance().getClass().getSimpleName() );

            appClass = Opt.of( getServletConfig().getInitParameter( "areca.appclass" ) )
                    .map( classname -> (Class<ServerApp>)Class.forName( classname ) )
                    .orElseThrow( () -> new ServletException( "No parameter: areca.appclass" ) );

            // call all static init()
            var reverse = new LinkedList<Method>();
            for (Class c = appClass; c != null; c = c.getSuperclass()) {
                Sequence.of( c.getMethods() )
                        .first( m -> m.getName().equals( "init" ) && Modifier.isStatic( m.getModifiers() ) )
                        .ifPresent( m -> reverse.addFirst( m ) );
            }
            for (var m : reverse) {
                var params = m.getParameterTypes();
                if (params.length == 1 && params[0].equals( ServletContext.class )) {
                    m.invoke( null, getServletContext() );
                }
                else {
                    m.invoke( null );
                }
            }

            Session.registerFactory( ServerApp.class, () -> appClass.newInstance() );
        }
        catch (Exception e) {
            throw new ServletException( e );
        }
    }


    @Override
    public void destroy() {
        try {
            if (appClass != null) {
                // find/call all static dispose()
                for (Class c = appClass; c != null; c = c.getSuperclass()) {
                    Sequence.of( c.getMethods() )
                            .first( m -> m.getName().equals( "dispose" ) && Modifier.isStatic( m.getModifiers() ) )
                            .ifPresent( m -> m.invoke( null ) );
                }
            }
        }
        catch (Exception e) {
            LOG.warn( "Error during destroy()", e );
        }
    }


    /**
     *
     */
    protected Session checkInitSession( HttpSession httpSession, boolean startSession ) {
        var session = (Session)httpSession.getAttribute( ATTR_SESSION );
        if (startSession) {
            synchronized (httpSession) {
                session = (Session)httpSession.getAttribute( ATTR_SESSION );
                if (session != null) {
                    LOG.info( "Session: DISPOSE" );
                    session.dispose();
                }
                LOG.info( "Session: START" );
                session = new Session();
                httpSession.setAttribute( ATTR_SESSION, session );

                sessionScope.bind( session, __ -> {
                    Session.setInstance( new UIEventCollector() ).start();

                    Session.setInstance( new EventLoop() ).enqueue( "createUI()", () -> {
                        Session.instanceOf( ServerApp.class ).createUI();
                    }, 0 );
                });
            }
        }
        return Assert.notNull( session );
    }


    /** Just testing with curl. */
    @Override
    protected void doGet( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException {
        doPost( req, resp );
    }


    @Override
    protected void doPost( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
        try {
            LOG.debug( "------ Request ------------------------------------------------------------");
            var t = Timer.start();

            // request: handle click events
            var _msg = (JsonClient2ServerMessage)null;
            try (var in = new InputStreamReader( request.getInputStream(), UTF_8 )) {
                _msg = gson.fromJson( in, JsonClient2ServerMessage.class );
                LOG.debug( "Received: %s", notPretty.toJson( _msg ));
            }
            var msg = _msg;

            // check/init session
            var httpSession = request.getSession( true );
            var session = checkInitSession( httpSession, msg != null ? msg.startSession : true );

            sessionScope.bind( session, __ -> {
                Assert.isSame( __, Session.current() );
                var eventLoop = __._instanceOf( EventLoop.class );
                var collector = __._instanceOf( UIEventCollector.class );

                // request: handle click events
                if (msg != null) {
                    eventLoop.enqueue( "client event", () -> {
                        for (var event : msg.events) {
                            // resize
                            if (event.eventType.equals( "resize" )) {
                                LOG.info( "RESIZE: %s", event.content );
                                var parts = StringUtils.split( event.content, ":" );
                                var size = Size.of( parseInt( parts[0] ), parseInt( parts[1] ) );
                                var component = event.componentId == 0
                                        ? collector.rootWindow()
                                        : collector.componentForId( event.componentId );
                                component.size.set( size );
                            }
                            // click, text, upload ...
                            else {
                                var component = collector.componentForId( event.componentId );
                                var eventType = EventType.valueOf( event.eventType );
                                if (component instanceof TextField) {
                                    Assert.isEqual( EventType.TEXT, eventType );
                                    ((TextField)component).content.rawSet( event.content );
                                }
                                else if (component instanceof ColorPicker) {
                                    Assert.isEqual( EventType.TEXT, eventType );
                                    ((ColorPicker)component).value.rawSet( event.content );
                                }
                                else if (component instanceof FileUpload) {
                                    Assert.isEqual( EventType.UPLOAD, eventType );

                                    var file = (FileUpload.File)httpSession.getAttribute( ATTR_UPLOADED );
                                    Assert.notNull( file, "No such uploaded file: " + event.content );
                                    Assert.isEqual( file.name(), event.content, "Uploaded file and name in event do not match." );

                                    ((FileUpload)component).data.rawSet( file );
                                    httpSession.removeAttribute( ATTR_UPLOADED );
                                }

                                component.events.values()
                                        .filter( handler -> handler.type.equals( eventType ) )
                                        .forEach( handler -> handler.consumer.accept( new ServerUIEvent( component, eventType ) ) );
                            }
                        }
                    }, 0 );
                }

                // response
                response.setBufferSize( 8*1024 );
                response.setCharacterEncoding( "UTF-8" );

                var c = new MutableInt();
                try (var out = response.getWriter()) {
                    out.write( "{\n  \"uiEvents\": [\n" );

                    collector.sink( ev -> {
                        out.write( c.getAndIncrement() == 0 ? "" : ",\n" );
                        gson.toJson( ev, out );
                    });

                    // eventloop
                    currentRequest.set( new Request( request, response ) );
                    eventLoop.execute();

                    out.write( String.format( "\n  ],\n  \"pendingWait\": %s\n}", eventLoop.pendingWait() ) );
                }
                finally {
                    collector.sink( null );
                    currentRequest.set( null );
                }
                LOG.info( "Sent: %s render events (%s)", c.getValue(), t.elapsedHumanReadable() );
            });
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * {@link FileUpload}
     */
    @Override
    protected void doPut( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException {
        var httpSession = req.getSession();
        if (httpSession == null) {
            resp.setStatus( HttpServletResponse.SC_FORBIDDEN );
            return;
        }
        var file = new FileUpload.File() {
            String mimetype = req.getContentType();
            String name = StringUtils.substringAfterLast( req.getPathInfo(), "/" );
            byte[] data = IOUtils.toByteArray( req.getInputStream() );  // XXX limit upload size

            @Override public byte[] data() { return data; }
            @Override public int size() { return data.length; }
            @Override public String mimetype() { return mimetype; }
            @Override public String name() { return name; }

            @Override
            public int lastModified() {
                throw new RuntimeException( "not yet implemented (on server side)" );
            }
            @Override
            public Object underlying() {
                throw new RuntimeException( "not yet implemented (on server side)" );
            }
        };
        httpSession.setAttribute( ATTR_UPLOADED, file );
        LOG.info( "PUT: complete (%s, %s, %s bytes)", file.name(), file.mimetype(), file.size() );
    }


    /**
     *
     */
    protected class ServerUIEvent extends UIEvent {

        public ServerUIEvent( UIComponent source, EventType type ) {
            super( source, null, type );
        }

        @Override
        public Position clientPos() {
            throw new RuntimeException( "not yet implemented." );
        }
    }

    /**
     *
     */
    public static class Request {
        public HttpServletRequest request;
        public HttpServletResponse response;

        protected Request( HttpServletRequest request, HttpServletResponse response ) {
            this.request = request;
            this.response = response;
        }
    }
}
