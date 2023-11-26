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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import areca.common.Assert;
import areca.common.Session;
import areca.common.SessionScoper;
import areca.common.SessionScoper.ThreadBoundSessionScoper;
import areca.common.Timer;
import areca.common.base.Opt;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.rt.server.EventLoop;
import areca.rt.server.ServerApp;
import areca.ui.Position;
import areca.ui.component2.Events.EventType;
import areca.ui.component2.Events.UIEvent;
import areca.ui.component2.UIComponent;

/**
 *
 * @author Falko Bräutigam
 */
public class ArecaUIServer
        extends HttpServlet {

    private static final Log LOG = LogFactory.getLog( ArecaUIServer.class );

    private Class<ServerApp> appClass;

    private ThreadBoundSessionScoper sessionScope = new ThreadBoundSessionScoper();

    private Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private Gson notPretty = new GsonBuilder().create();


    @Override
    @SuppressWarnings({"unchecked", "deprecation"})
    public void init() throws ServletException {
        SessionScoper.setInstance( sessionScope );
        try {
            appClass = Opt.of( getServletConfig().getInitParameter( "areca.appclass" ) )
                    .map( classname -> (Class<ServerApp>)Class.forName( classname ) )
                    .orElseThrow( () -> new ServletException( "No parameter: areca.appclass" ) );
            var init = appClass.getMethod( "init", new Class[0] );
            init.invoke( null );

            Session.registerFactory( ServerApp.class, () -> appClass.newInstance() );
        }
        catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new ServletException( e );
        }
    }


    /**
     *
     */
    protected Session checkInitSession( HttpSession httpSession, boolean startSession ) {
        var session = (Session)httpSession.getAttribute( "areca.session" );
        if (startSession) {
            synchronized (httpSession) {
                if (session != null) {
                    LOG.info( "Session: dispose!" );
                    session.dispose();
                }
                LOG.info( "Session: START!" );
                session = new Session();
                httpSession.setAttribute( "areca.session", session );

                sessionScope.bind( session, __ -> {
                    Session.setInstance( new EventLoop() );
                    Session.setInstance( new UIEventCollector() ).start();
                    Session.instanceOf( ServerApp.class ).createUI( );
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
            LOG.info( "------ Request ------------------------------------------------------------");
            var t = Timer.start();

            // request: handle click events
            var _msg = (JsonClient2ServerMessage)null;
            try (var in = new InputStreamReader( request.getInputStream(), UTF_8 )) {
                if (in.ready()) {
                    _msg = gson.fromJson( in, JsonClient2ServerMessage.class );
                    LOG.info( "Received: %s", notPretty.toJson( _msg ));
                }
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
                    eventLoop.enqueue( "client click", () -> {
                        for (var event : msg.events) {
                            var component = collector.componentForId( event.componentId );
                            var eventType = EventType.valueOf( event.eventType );
                            component.events.values()
                                    .filter( handler -> handler.type.equals( eventType ) )
                                    .forEach( handler -> handler.consumer.accept( new ServerUIEvent( component, eventType ) ) );
                        }
                    }, 0 );
                }

                // event loop
                eventLoop.execute( -1 );

                // response
                var responseMsg = new JsonServer2ClientMessage();
                responseMsg.uiEvents = collector.events();
                responseMsg.pendingWait = (int)eventLoop.pendingWait();

                try (var out = new OutputStreamWriter( response.getOutputStream(), UTF_8 )) {
                    gson.toJson( responseMsg, out );
                }
                LOG.info( "UI events send: %s (%s)", responseMsg.uiEvents.size(), t.elapsedHumanReadable() );
            });
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     *
     */
    protected class ServerUIEvent extends UIEvent {

        public ServerUIEvent( UIComponent source, EventType type ) {
            super( source );
            this.type = type;
        }

        @Override
        public Position clientPos() {
            // XXX Auto-generated method stub
            throw new RuntimeException( "not yet implemented." );
        }
    }
}