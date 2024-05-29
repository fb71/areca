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

    /**
     * The current state in the browser history.
     * Prevents Browser and Pageflow close events to cycle.
     */
    protected volatile int  browserHistoryState;


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
        var state = Integer.parseInt( ev.state );
        LOG.warn( "Browser event: state = %s, browserHistoryState = %s", state, browserHistoryState );

        for (int s = browserHistoryState; s > state; s--) {
            pageflow.close( pageflow.pages().first().get() );
        }
    }


    protected void onPageflowEvent( PageflowEvent ev ) {
        if (ev.type == EventType.PAGE_OPENED || ev.type == EventType.PAGE_CLOSED) {
            var pageCount = pageflow.pages().count();
            LOG.warn( "Pageflow event: type = %s, browserHistoryState = %s, pageCount = %s", ev.type, browserHistoryState, pageCount );
            if (browserHistoryState != pageCount) {
                browserHistoryState = pageCount;

                var _ev = new JsonUIComponentEvent( "Pageflow" );
                _ev.propName = ev.type.toString();
                _ev.propNewValue = JsonServer2ClientMessage.encodeValue( browserHistoryState );
                Session.instanceOf( UIEventCollector.class ).add( _ev );
            }
        }
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


//    /**
//     * Skip one (the next) subsequent event.
//     */
//    @NoRuntimeInfo
//    private void skipSubsequentEvent( Runnable task ) {
//        if (skipCloseEvent-- > 0) {
//            return;
//        }
//        skipCloseEvent = 1;
//        task.run();
//    }

}
