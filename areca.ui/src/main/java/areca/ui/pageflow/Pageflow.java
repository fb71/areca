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
import java.util.HashSet;
import java.util.Set;

import areca.common.Assert;
import areca.common.Platform;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Position;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
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

    private static class PageData {
        Page        page;
        PageSite    site;
        UIComponent container;
    }

    private static class ScopedData {
        String      scope;
        Object      data;
    }

    // instance *******************************************

    private UIComposite         rootContainer;

    private Deque<PageData>     pages = new ArrayDeque<>();

    /* XXX this probably to simple; data should life for its page only!? */
    protected Set<ScopedData>   data = new HashSet<>();


    protected Pageflow( UIComposite rootContainer ) {
        this.rootContainer = rootContainer;
        this.rootContainer.layout.set( new PageStackLayout() );
        //new PageCloseGesture( rootContainer );
    }


    public void open( Page _page, Page parent, Position origin ) {
        Assert.that( pages.isEmpty() || parent == pages.peek().page, "Adding other than top page is not supported yet." );
        var _pageSite = new PageSite( _page ) {
            @Override
            @SuppressWarnings("unchecked")
            public <R> R data( Class<R> type, String scope ) {
                return (R)Sequence.of( data )
                        .first( sd -> type.isInstance( sd.data ) && scope.equals( sd.scope ) )
                        .orElseThrow( () -> new IllegalStateException( "No data for type: " + type.getSimpleName() + ", scope: " + scope ) )
                        .data;
            }
            @Override
            public PageSite put( Object _data, String _scope ) {
                var scopedData = new ScopedData() {{
                    data = Assert.notNull( _data );
                    scope = Assert.notNull( _scope );
                }};
                if (!data.add( scopedData )) {
                    throw new IllegalArgumentException( "Data is already there: " + _data.getClass().getSimpleName() + ", scope: " + _scope );
                }
                return this;
            }
        };
        var _pageContainer = _page.init( rootContainer, _pageSite );
        pages.push( new PageData() {{page = _page; site = _pageSite; container = _pageContainer;}} );

        var layout = (PageStackLayout)rootContainer.layout.value();
        layout.layout( rootContainer ); // do NOT layout ALL child components

        if (_pageContainer instanceof UIComposite) {
            ((UIComposite)_pageContainer).layout();
        }
        layout.openLast( origin );
    }


    public void close( Page page ) {
        Assert.isSame( page, pages.peek().page, "Removing other than top page is not supported yet." );
        var pageData = pages.pop();
        pageData.container.cssClasses.add( "Closing" );
        with( pageData.container.position ).apply( pos -> pos.set(
                Position.of( pos.value().x, rootContainer.clientSize.value().height() - 30 ) ) );

        Platform.schedule( 750, () -> {
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
            on( ev -> {
                var top = pages.peek().container;
                LOG.debug( "PageCloseGesture: top = %s, status = %s, delta = %s", top, ev.status(), "???"); //ev.delta() != null ? ev.delta() : "???" );
                switch (ev.status()) {
                    case START: {
                        startPos = top.position.value();
                        top.cssClasses.add( "Paning" );
                        break;
                    }
                    case MOVE: {
                        //top.bordered.set( true );
                        top.position.set( Position.of( startPos.x, startPos.y + ev.delta().y ) );
                        top.opacity.set( Math.max( 0.2f, (PEEK_DISTANCE_PX - ev.delta().y) / PEEK_DISTANCE_PX ) );
                        break;
                    }
                    case END: {
                        top.opacity.set( null );
                        top.cssClasses.remove( "Paning" );

                        // close
                        if (ev.clientPos().y > (component.clientSize.value().height() - EDGE_THRESHOLD)) {
                            Pageflow.current().close( pages.peek().page );
                        }
                        // reset
                        else {
                            top.position.set( startPos );
                            //Platform.schedule( 750, () -> top.bordered.set( false ) );
                        }
                    }
                }
            });
        }
    }
}
