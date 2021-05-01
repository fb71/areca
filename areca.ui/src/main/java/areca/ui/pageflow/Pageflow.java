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
package areca.ui.pageflow;

import static areca.common.base.With.with;

import java.util.ArrayDeque;
import java.util.Deque;

import areca.common.Assert;
import areca.common.Platform;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Position;
import areca.ui.component.UIComponent;
import areca.ui.component.UIComposite;
import areca.ui.gesture.PanGesture;
import areca.ui.pageflow.Page.PageSite;

/**
 *
 * @author Falko Bräutigam
 */
public class Pageflow {

    private static final Log LOG = LogFactory.getLog( Pageflow.class );

    private static Pageflow     instance;

    public static Pageflow start( UIComposite rootContainer ) {
        Assert.isNull( instance );
        return instance = new Pageflow( rootContainer );
    }

    public static Pageflow current() {
        return Assert.notNull( instance, "Pageflow not start()ed yet." );
    }

    private class PageData {
        Page        page;
        PageSite    site;
        UIComponent container;
    }

    // instance *******************************************

    private UIComposite         rootContainer;

    private Deque<PageData>     pages = new ArrayDeque<>();


    protected Pageflow( UIComposite rootContainer ) {
        this.rootContainer = rootContainer;
        this.rootContainer.layout.set( new PageStackLayout() );
        new PageCloseGesture( rootContainer );
    }


    public void open( Page _page, Page parent ) {
        Assert.that( pages.isEmpty() || parent == pages.peek().page, "Adding other than top page is not supported yet." );
        var _pageSite = new PageSite() {{
        }};
        var _pageContainer = _page.init( rootContainer, _pageSite );
        pages.push( new PageData() {{page = _page; site = _pageSite; container = _pageContainer;}} );
        rootContainer.layout();
    }


    public void close( Page page ) {
        Assert.isSame( page, pages.peek().page, "Removing other than top page is not supported yet." );
        var pageData = pages.pop();
        pageData.container.cssClasses.add( "Closing" );
        with( pageData.container.position).apply( pos -> pos.set(
                Position.of( pos.get().x, rootContainer.clientSize.get().height() - 30 ) ) );

        Platform.instance().schedule( 750, () -> {
            pageData.page.dispose();
            if (!pageData.container.isDisposed()) {
                pageData.container.dispose();
            }
            // rootContainer.layout();
        });
    }


    /**
     * Close/peek the top page of the {@link Pageflow}.
     */
    class PageCloseGesture
            extends PanGesture {

        private static final float PEEK_DISTANCE_PX = 200f;

        private Position startPos;

        public PageCloseGesture( UIComposite component ) {
            super( component );
            onEvent( ev -> {
                LOG.info( "Gesture: %s", ev.delta.get() );
                var top = component.components.sequence().last().get();
                switch (ev.status.get()) {
                    case START: {
                        startPos = top.position.get();
                        top.cssClasses.add( "Paning" );
                        break;
                    }
                    case MOVE: {
                        top.bordered.set( true );
                        top.position.set( Position.of(
                                startPos.x,
                                startPos.y + ev.delta.get().y ) );
                        float opacity = Math.max( 0.2f, (PEEK_DISTANCE_PX - ev.delta.get().y) / PEEK_DISTANCE_PX );
                        top.htmlElm.styles.set( "opacity", opacity );
                        break;
                    }
                    case END: {
                        top.htmlElm.styles.remove( "opacity" );
                        top.cssClasses.remove( "Paned" );

                        // close
                        if (ev.clientPos.get().y > (component.clientSize.get().height() - EDGE_THRESHOLD)) {
                            Pageflow.current().close( pages.peek().page );
                        }
                        // reset
                        else {
                            top.position.set( startPos );
                            Platform.instance().schedule( 1000, () -> top.bordered.set( false ) );
                        }
                    }
                }
            });
        }
    }
}
