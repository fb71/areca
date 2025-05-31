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
package areca.rt.server;

import java.util.EventObject;
import areca.common.Session;
import areca.common.Timer;
import areca.common.event.EventManager;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.rt.server.client.ClientBrowserHistoryStrategy;
import areca.rt.server.servlet.JsonServer2ClientMessage;
import areca.rt.server.servlet.JsonServer2ClientMessage.JsonUIComponentEvent;
import areca.rt.server.servlet.UIEventCollector;
import areca.ui.pageflow.Pageflow;
import areca.ui.pageflow.PageflowEvent;
import areca.ui.pageflow.PageflowEvent.EventType;

/**
 * Handles Browser back button.
 *
 * @see ClientBrowserHistoryStrategy
 * @author Falko Bräutigam
 */
public class ServerBrowserHistoryStrategy {

    private static final Log LOG = LogFactory.getLog( ServerBrowserHistoryStrategy.class );

    public static ServerBrowserHistoryStrategy start( Pageflow pageflow ) {
        return new ServerBrowserHistoryStrategy( pageflow );
    }

    // instance *******************************************

    protected Pageflow      pageflow;

    protected Timer         lastBrowserEvent = Timer.start();


    protected ServerBrowserHistoryStrategy( Pageflow pageflow ) {
        this.pageflow = pageflow;
        EventManager.instance()
                .subscribe( ev -> onPageflowEvent( (PageflowEvent)ev ) )
                .performIf( PageflowEvent.class, ev -> ev.getSource() == this.pageflow )
                .unsubscribeIf( () -> pageflow.isDisposed() );
        EventManager.instance()
                .subscribe( ev -> onBrowserHistoryEvent( (BrowserHistoryEvent)ev ) )
                .performIf( BrowserHistoryEvent.class, ev -> true )
                .unsubscribeIf( () -> pageflow.isDisposed() );
    }


    protected void onBrowserHistoryEvent( BrowserHistoryEvent ev ) {
        LOG.debug( "BROWSER event: state = %s", ev.state );
        var state = Integer.parseInt( ev.state );

        var pages = pageflow.pages().toArray( Object[]::new );
        for (int i = 0; i < pages.length; i++) {
            if (pageId( pages[i] ) == state) {
                lastBrowserEvent.restart();
                pageflow.close( pages[i + 1] );
            }
        }
    }


    protected void onPageflowEvent( PageflowEvent ev ) {
        LOG.debug( "PAGEFLOW event: type = %s", ev.type );

        var pageId = Integer.valueOf( pageId( ev.clientPage ) );
        var browserEvent = new JsonUIComponentEvent( "Pageflow" );
        browserEvent.propName = ev.type.toString();
        browserEvent.propNewValue = JsonServer2ClientMessage.encodeValue( pageId );

        if (ev.type == EventType.PAGE_OPENED) {
            Session.instanceOf( UIEventCollector.class ).add( browserEvent );
        }
        //
        else if (ev.type == EventType.PAGE_CLOSED) {
            if (lastBrowserEvent.elapsedMillis() > 1000) {
                Session.instanceOf( UIEventCollector.class ).add( browserEvent );
            }
            else {
                LOG.debug( "    skipped. (%s)", lastBrowserEvent.elapsedHumanReadable() );
            }
        }
    }


    protected int pageId( Object clientPage ) {
        return Math.abs( System.identityHashCode( clientPage ) % 10000 );
    }


    /**
     *
     */
    public static class BrowserHistoryEvent
            extends EventObject {

        private String state;

        public BrowserHistoryEvent( String type, String state ) {
            super( type );
            this.state = state;
        }

        @Override
        public String getSource() {
            return (String)super.getSource();
        }
    }

}
