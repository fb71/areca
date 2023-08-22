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
package areca.rt.teavm;

import org.teavm.jso.browser.Window;

import areca.common.Assert;
import areca.common.event.EventHandler;
import areca.common.event.EventManager;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.NoRuntimeInfo;
import areca.common.reflect.RuntimeInfo;
import areca.rt.teavm.PopStateEvent.BrowserHistoryState;
import areca.ui.pageflow.Pageflow;
import areca.ui.pageflow.PageflowEvent;
import areca.ui.pageflow.PageflowEvent.EventType;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class SimpleBrowserHistoryStrategy {

    private static final Log LOG = LogFactory.getLog( SimpleBrowserHistoryStrategy.class );

    public static final ClassInfo<SimpleBrowserHistoryStrategy> INFO = SimpleBrowserHistoryStrategyClassInfo.instance();

    public static SimpleBrowserHistoryStrategy start( Pageflow pageflow ) {
        return new SimpleBrowserHistoryStrategy( pageflow );
    }

    // instance *******************************************

    protected Pageflow      pageflow;

    /** Prevent Browser and Pageflow close events to cycle. */
    protected volatile int  skipCloseEvent;


    protected SimpleBrowserHistoryStrategy() {}


    protected SimpleBrowserHistoryStrategy( Pageflow pageflow ) {
        this.pageflow = pageflow;
        EventManager.instance().subscribe( this )
                .performIf( PageflowEvent.class, ev -> ev.getSource() == this.pageflow )
                .unsubscribeIf( () -> pageflow.isDisposed() );

        Window.current().addEventListener( "popstate", ev -> onBrowserHistoryEvent( ev.cast() ) );
    }


    protected void onBrowserHistoryEvent( PopStateEvent ev ) {
        ev.preventDefault();

        BrowserHistoryState bhs = ev.getState().cast();
        var state = bhs != null ? Integer.parseInt( bhs.getState() ) : 1;
        LOG.debug( "Browser event: state = %s", state );

        //
        //Assert.that( pageflow.pages().count() > state );
        skipSubsequentEvent( () -> pageflow.close( pageflow.pages().first().get() ) );
    }


    @EventHandler(PageflowEvent.class)
    protected void onPageflowEvent( PageflowEvent ev ) {
        LOG.debug( "Pageflow event: type = %s", ev.type );

        var pageCount = pageflow.pages().count();
        if (ev.type == EventType.PAGE_OPENED) {
            Assert.that( pageCount > 1, "we assume that this is initialized after pageflow was created and first page opened" );
            Window.current().getHistory().pushState( BrowserHistoryState.create( ""+pageCount ), "", "#"+pageCount );
        }
        else if (ev.type == EventType.PAGE_CLOSED) {
            skipSubsequentEvent( () -> Window.current().getHistory().back() );
        }
    }


    /**
     * Skip one (the next) subsequent event.
     */
    @NoRuntimeInfo
    private void skipSubsequentEvent( Runnable task ) {
        if (skipCloseEvent-- > 0) {
            return;
        }
        skipCloseEvent = 1;
        task.run();
    }

}
