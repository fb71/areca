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

import static areca.rt.server.client.JSServer2ClientMessage.VALUE_MISSING;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.teavm.jso.json.JSON;

import areca.common.Assert;
import areca.common.Platform;
import areca.common.Platform.HttpResponse;
import areca.common.Promise;
import areca.common.Promise.CancelledException;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.rt.server.client.JSClient2ServerMessage.JSClickEvent;
import areca.rt.server.servlet.ArecaUIServer;
import areca.ui.App.RootWindow;
import areca.ui.component2.Button;
import areca.ui.component2.Events.EventType;
import areca.ui.component2.Events.UIEvent;
import areca.ui.component2.Label;
import areca.ui.component2.Link;
import areca.ui.component2.Property.PropertyChangedEvent;
import areca.ui.component2.Property.ReadWrite;
import areca.ui.component2.ScrollableComposite;
import areca.ui.component2.Text;
import areca.ui.component2.TextField;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComponentDecorator;
import areca.ui.component2.UIComponentEvent.ComponentAttachedEvent;
import areca.ui.component2.UIComponentEvent.ComponentConstructingEvent;
import areca.ui.component2.UIComponentEvent.ComponentDetachedEvent;
import areca.ui.component2.UIComponentEvent.ComponentDisposedEvent;
import areca.ui.component2.UIComponentEvent.DecoratorAttachedEvent;
import areca.ui.component2.UIComponentEvent.DecoratorDetachedEvent;
import areca.ui.component2.UIComposite;
import areca.ui.component2.UIElement;
import areca.ui.pageflow.PageContainer;

/**
 * The client (browser) side connection to an {@link ArecaUIServer}.
 *
 * @author Falko Bräutigam
 */
public class Connection {

    private static final Log LOG = LogFactory.getLog( Connection.class );

    private static final String SERVER_PATH = "eventloop";

    private static final String PACKAGE_UI = "areca.ui";
    private static final String PACKAGE_UI_COMPONENTS = PACKAGE_UI + ".component2";
    private static final String PACKAGE_UI_PAGEFLOW = PACKAGE_UI + ".pageflow";

    /** The RootWindow on the client side */
    private UIComposite                 rootWindow;

    private Map<Integer,UIElement>      components = new HashMap<>( 512 );

    /** Pending events to be send to the server (click, text, resize, ..) */
    private Deque<JSClickEvent>         clickEvents = new ArrayDeque<>();

    private Promise<?>                  pendingWait;

    private Promise<HttpResponse>       pendingRequest;

    private Promise<Void>               clientEventThrottle;

    private boolean                     isStarted;


    public Connection( UIComposite rootWindow ) {
        this.rootWindow = rootWindow;
    }


    public Connection start() {
        readServer( true );
        isStarted = true;
        return this;
    }


    protected void readServer( boolean startSession ) {
        var send = JSClient2ServerMessage.create();
        send.setStartSession( startSession );
        send.setEvents( Sequence.of( clickEvents ).toArray( JSClickEvent[]::new ) );
        clickEvents.clear();
        var json = JSON.stringify( send );
        LOG.warn( "Sending request: %s", json );
        pendingRequest = Platform.xhr( "POST", SERVER_PATH )
                .submit( json )
                .onSuccess( response -> {
                    pendingRequest = null;
                    try {
                        var msg = (JSServer2ClientMessage)JSON.parse( response.text() );

                        processUIEvents( msg );

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


    protected void processUIEvents( JSServer2ClientMessage msg ) {
        LOG.info( "Received: %s render events", msg.uiEvents().length );
        for (var ev : msg.uiEvents()) {
            //LOG.info( "Event: %s", ev.eventType() );

            // constructing
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
                var component = (UIComponent)components.get( ev.componentId() );
                parent.add( component );
            }
            // detached
            else if (ComponentDetachedEvent.class.getSimpleName().equals( ev.eventType() )) {
                var component = (UIComponent)components.get( ev.componentId() );
                //Assert.isEqual( ev.parentId(), component.parent().id() );
                component.parent().components.remove( component ).orElseError();
            }
            // disposed
            else if (ComponentDisposedEvent.class.getSimpleName().equals( ev.eventType() )) {
                components.remove( ev.componentId() ).dispose();
            }
            // property
            else if (PropertyChangedEvent.class.getSimpleName().equals( ev.eventType() )) {
                var element = components.get( ev.componentId() );
                var prop = Assert.notNull( element.propertyForName( ev.propName() ) );

                @SuppressWarnings("unchecked")
                var rw = (ReadWrite<?,Object>)prop;
                var value = JSServer2ClientMessage.decodeValue( ev.propNewValue().cast() );

                if (value == VALUE_MISSING) {
                }
                // Events
                else if (value instanceof List
                        && !((List)value).isEmpty()
                        && ((List)value).get( 0 ) instanceof EventType) {
                    var component = (UIComponent)element;
                    Assert.that( component.events.$().isEmpty(), "..." );
                    for (var v : (List)value) {
                        component.events.on( (EventType)v, _ev -> {
                            onComponentEvent( component, _ev );
                        });
                    }
                }
                // normal
                else {
                    rw.set( value );
                }
            }
            // decorator attached
            else if (DecoratorAttachedEvent.class.getSimpleName().equals( ev.eventType() )) {
                var component = Assert.notNull( (UIComponent)components.get( ev.parentId() ), "No such component: " + ev.parentId() );
                var decorator = Assert.notNull( (UIComponentDecorator)components.get( ev.componentId() ) );
                component.addDecorator( decorator );
            }
            // decorator detached
            else if (DecoratorDetachedEvent.class.getSimpleName().equals( ev.eventType() )) {
                var component = Assert.notNull( (UIComponent)components.get( ev.parentId() ), "No such component: " + ev.parentId() );
                var decorator = Assert.notNull( (UIComponentDecorator)components.get( ev.componentId() ) );
                component.decorators.remove( decorator );
            }
            else {
                throw new RuntimeException( "mas trabajo: " + ev.eventType() );
            }
        }
    }

    /**
     * Called when the user has clicked on a component or any other {@link EventType}.
     */
    protected void onComponentEvent( UIComponent component, UIEvent ev ) {
        var jsev = JSClickEvent.create();
        jsev.setEventType( ev.type.toString() );
        jsev.setComponentId( component.id() );
        //jsev.setPosition( )

        int delay = 0;
        if (ev.type == EventType.TEXT) {
            jsev.setContent( ((TextField)component).content.get() );
            delay = 250;
        }

        clickEvents.add( jsev );
        sendClientEvents( delay );
    }


    protected void sendClientEvents( int throttleDelay ) {
        if (clientEventThrottle != null) {
            clientEventThrottle.cancel();
        }
        clientEventThrottle = Platform.schedule( throttleDelay, () -> {
            LOG.info( "THROTTLE: %s events", clickEvents.size() );

            Assert.isNull( pendingRequest );
            if (pendingWait != null) {
                pendingWait.cancel();
                pendingWait = null;
            }
            readServer( false );

            clientEventThrottle = null;
            return null;
        });
        clientEventThrottle.onError( e -> {
            // prevent default error handler
            if (!(e instanceof CancelledException)) {
                throw (RuntimeException)e;
            }
        });
    }



    public void enqueueClickEvent( JSClickEvent ev ) {
        clickEvents.add( ev );
        if (isStarted) {
            sendClientEvents( 0 );
        }
    }


    private UIElement createInstance( String classname ) {
        switch (classname) {
            case PACKAGE_UI_COMPONENTS + ".UIComposite" : return new UIComposite();
            case PACKAGE_UI_COMPONENTS + ".ScrollableComposite" : return new ScrollableComposite();
            case PACKAGE_UI_COMPONENTS + ".Text" : return new Text();
            case PACKAGE_UI_COMPONENTS + ".Button" : return new Button();
            case PACKAGE_UI_COMPONENTS + ".TextField" : return new TextField();
            case PACKAGE_UI_COMPONENTS + ".Link" : return new Link();
            case PACKAGE_UI_COMPONENTS + ".Label" : return new Label();
            case PACKAGE_UI_PAGEFLOW + ".PageContainer" : return new PageContainer();
            default: {
                LOG.warn( "fehlt noch: " + classname );
                throw new RuntimeException( "fehlt noch: " + classname );
            }
        }
    }

}
