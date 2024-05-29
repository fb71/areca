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

import java.util.EventObject;

import org.teavm.jso.browser.Window;

import areca.common.Session;
import areca.common.event.EventManager;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.rt.server.ServerBrowserHistoryStrategy;
import areca.rt.server.client.JSClient2ServerMessage.JSClickEvent;
import areca.rt.teavm.PopStateEvent;
import areca.rt.teavm.PopStateEvent.BrowserHistoryState;

/**
 * The client side part of the {@link ServerBrowserHistoryStrategy}.
 *
 * @author Falko Bräutigam
 */
public class ClientBrowserHistoryStrategy {

    private static final Log LOG = LogFactory.getLog( ClientBrowserHistoryStrategy.class );

    public static ClientBrowserHistoryStrategy start() {
        return new ClientBrowserHistoryStrategy();
    }


    protected ClientBrowserHistoryStrategy() {
        Window.current().addEventListener( "popstate", ev -> onBrowserHistoryEvent( ev.cast() ) );
        EventManager.instance()
                .subscribe( ev -> onPageflowEvent( (PageflowEvent)ev ) )
                .performIf( PageflowEvent.class, ev -> true );
    }


    protected void onPageflowEvent( PageflowEvent ev ) {
        if (ev.getSource().equals( "PAGE_OPENED" )) {
            var state = String.valueOf( ev.pageCount );
            Window.current().getHistory().pushState( BrowserHistoryState.create( state ), "", "#"+state );
        }
        else if (ev.getSource().equals( "PAGE_CLOSED" )) {
            var current = Window.current().getHistory().getState().<BrowserHistoryState>cast();
            LOG.debug( "onPageflowEvent(): %s, pageCount = %s, current history = %s", ev.getSource(), ev.pageCount, current.getState() );

            // prevent cycles
            if (ev.pageCount < Integer.parseInt( current.getState() )) {
                Window.current().getHistory().back();
            }
        }
    }


    protected void onBrowserHistoryEvent( PopStateEvent ev ) {
        ev.preventDefault();
        ev.stopPropagation();

        BrowserHistoryState bhs = ev.getState().cast();
        var state = bhs != null ? Integer.parseInt( bhs.getState() ) : 1;
        LOG.debug( "onBrowserEvent(): popped state = %s", state );

        var conn = Session.instanceOf( Connection.class );
        conn.enqueueClickEvent( JSBrowserHistoryEvent.create( state ) );
    }

    /**
     *
     */
    public static abstract class JSBrowserHistoryEvent
            extends JSClickEvent {

        public static JSBrowserHistoryEvent create( int state ) {
            var result = JSClickEvent.create();
            result.setEventType( "BrowserHistory.popstate" );
            result.setContent( String.format( "%s", state ) );
            return result.cast();
        }
    }

    /**
     *
     */
    public static class PageflowEvent
            extends EventObject {

        public int pageCount;

        public PageflowEvent( String type, int pageCount ) {
            super( type );
            this.pageCount = pageCount;
        }

        @Override
        public String getSource() {
            return (String)super.getSource();
        }
    }

}
