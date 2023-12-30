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
package areca.rt.server.client;

import static org.apache.commons.lang3.StringUtils.split;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import org.teavm.jso.json.JSON;

import areca.common.Assert;
import areca.common.Platform;
import areca.common.Platform.HttpResponse;
import areca.common.Promise;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.rt.server.client.JSClient2ServerMessage.JSClickEvent;
import areca.rt.server.servlet.ArecaUIServer;
import areca.ui.App.RootWindow;
import areca.ui.component2.Button;
import areca.ui.component2.Events.EventType;
import areca.ui.component2.Events.UIEvent;
import areca.ui.component2.Property.PropertyChangedEvent;
import areca.ui.component2.Text;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComponentEvent.ComponentAttachedEvent;
import areca.ui.component2.UIComponentEvent.ComponentConstructingEvent;
import areca.ui.component2.UIComponentEvent.ComponentDetachedEvent;
import areca.ui.component2.UIComponentEvent.ComponentDisposedEvent;
import areca.ui.component2.UIComposite;
import areca.ui.pageflow.PageContainer;

/**
 * The client (browser) side connection to an {@link ArecaUIServer}.
 *
 * @author Falko Bräutigam
 */
public class Connection {

    private static final Log LOG = LogFactory.getLog( Connection.class );

    private static final String PACKAGE_UI = "areca.ui";
    private static final String PACKAGE_UI_COMPONENTS = PACKAGE_UI + ".component2";
    private static final String PACKAGE_UI_PAGEFLOW = PACKAGE_UI + ".pageflow";

    /** The RootWindow on the client side */
    private UIComposite                 rootWindow;

    private Map<Integer,UIComponent>    components = new HashMap<>( 512 );

    /** Pending (click) events to be send to the server. */
    private Deque<JSClickEvent>         clickEvents = new ArrayDeque<>();

    private Promise<?>                  pendingWait;

    private Promise<HttpResponse>       pendingRequest;


    public Connection( UIComposite rootWindow ) {
        this.rootWindow = rootWindow;
    }


    public void start() {
        readServer( true );
    }


    protected void readServer( boolean startSession ) {
        var send = JSClient2ServerMessage.create();
        send.setStartSession( startSession );
        send.setEvents( Sequence.of( clickEvents ).toArray( JSClickEvent[]::new ) );
        clickEvents.clear();
        var json = JSON.stringify( send );
        LOG.info( "Sending request: %s", json );
        pendingRequest = Platform.xhr( "POST", "ui" )
                .submit( json )
                .onSuccess( response -> {
                    pendingRequest = null;
                    try {
                        var msg = (JSServer2ClientMessage)JSON.parse( response.text() );
                        readUIEvents( msg );
                        pendingWait = msg.pendingWait() >= 0
                                ? Platform.schedule( msg.pendingWait(), () -> readServer( false ) )
                                : null;
                    }
                    catch (Exception e) {
                        LOG.warn( e.getMessage(), e );
                        throw e;
                    }
                })
                .onError( e -> {
                    pendingRequest = null;
                });
    }


    protected void readUIEvents( JSServer2ClientMessage msg ) {
        LOG.info( "Server message: uiEvents = %s", msg.uiEvents().length );
        for (var ev : msg.uiEvents()) {
            //LOG.info( "Event: %s", ev.eventType() );

            // constructed
            if (ComponentConstructingEvent.class.getSimpleName().equals( ev.eventType() )) {
                var component = ev.componentClass().equals( RootWindow.class.getName() )
                        ? rootWindow
                        : createInstance( ev.componentClass() );
                component.setId( ev.componentId() );
                Assert.isNull( components.put( component.id(), component ) );
            }
            // attached
            else if (ComponentAttachedEvent.class.getSimpleName().equals( ev.eventType() )) {
                var parent = (UIComposite)components.get( ev.parentId() );
                var component = components.get( ev.componentId() );
                parent.add( component );
            }
            // detached
            else if (ComponentDetachedEvent.class.getSimpleName().equals( ev.eventType() )) {
                var component = components.get( ev.componentId() );
                //Assert.isEqual( ev.parentId(), component.parent().id() );
                component.parent().components.remove( component ).orElseError();
            }
            // disposed
            else if (ComponentDisposedEvent.class.getSimpleName().equals( ev.eventType() )) {
                var component = components.remove( ev.componentId() );
                component.dispose();
            }
            // property
            else if (PropertyChangedEvent.class.getSimpleName().equals( ev.eventType() )) {
                var component = components.get( ev.componentId() );
                var prop = Sequence.of( component.allProperties() )
                        .first( p -> p.name().equals( ev.propName() ) )
                        .orElseError();
                // Events
                if (ev.propNewValue().startsWith( "E:")) {
                    Assert.that( ev.propOldValue().length() <= 2 ); // no old values
                    var value = ev.propNewValue().substring( 2 );
                    for (var eventTypeName : split( value, ":" )) {
                        var eventType = EventType.valueOf( eventTypeName );
                        component.events.on( eventType, _ev -> {
                            onClick( component, _ev );
                        });
                    }
                }
                // component + property
                else {
                    PropertyValueCoder.decode( prop, ev.propNewValue() );
                }
            }
            else {
                throw new RuntimeException( "mehr arbeit: " + ev.eventType() );
            }
        }
    }


    /**
     * Called when the user has clicked on a component or any other {@link EventType}.
     */
    protected void onClick( UIComponent component, UIEvent ev ) {
        var jsev = JSClickEvent.create();
        jsev.setEventType( ev.type.toString() );
        jsev.setComponentId( component.id() );
        //jsev.setPosition( )
        clickEvents.add( jsev );

        Assert.isNull( pendingRequest );
        if (pendingWait != null) {
            pendingWait.cancel();
            pendingWait = null;
        }
        readServer( false );
    }



    private UIComponent createInstance( String classname ) {
        switch (classname) {
            case PACKAGE_UI_COMPONENTS + ".UIComposite" : return new UIComposite();
            case PACKAGE_UI_COMPONENTS + ".Text" : return new Text();
            case PACKAGE_UI_COMPONENTS + ".Button" : return new Button();
            case PACKAGE_UI_PAGEFLOW + ".PageContainer" : return new PageContainer();
            default: {
                LOG.warn( "fehlt noch: " + classname );
                throw new RuntimeException( "fehlt noch: " + classname );
            }
        }
    }

}
