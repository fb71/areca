/*
 * Copyright (C) 2021-2023, the @authors. All rights reserved.
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
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import areca.common.Assert;
import areca.common.Platform;
import areca.common.base.Opt;
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

    /**
     *
     */
    protected static class Scoped {
        String      scope;
        Object      value;

        public Scoped( Object value, String scope ) {
            this.scope = Assert.notNull( scope, "Scope must not be null. Use DEFAULT_SCOPE instead." );
            this.value = Assert.notNull( value, "Value of a context variable must not be null. Removing is not yet supported." );
        }

        public boolean isCompatible( Class<?> type, @SuppressWarnings("hiding") String scope ) {
            return type.isAssignableFrom( value.getClass() ) && scope.equals( this.scope );
        }
    }

    /**
     *
     */
    protected class PageHolder extends PageSite {

        /** The root component of the UI of this {@link Page} */
        UIComponent     ui;
        /** Page supplied by client code or {@link AnnotatedPage}. */
        Page            page;
        /** The page impl supplied by client code, Pojo or {@link Page}. */
        Object          clientPage;

        PageHolder      parent;

        List<Scoped>    context = new ArrayList<>();


        @Override
        public String toString() {
            return String.format( "PageSite[page=%s, context=[%s]]", clientPage.getClass().getSimpleName(),
                    Sequence.of( context )
                            .map( scoped -> scoped.value )
                            .map( value -> value instanceof PageSite ? "-PageSite-" : value.toString() )
                            .reduce( (r,elm) -> r + ", " ).orElse( "" ) );
        }

        @Override
        public PageBuilder createPage( Object newPage ) {
            return Pageflow.this.create( newPage ).parent( clientPage );
        }

        @Override
        public void close() {
            Pageflow.this.close( clientPage );
        }

        protected Opt<Scoped> _local( Class<?> type, String scope ) {
            return Sequence.of( context ).first( scoped -> scoped.isCompatible( type, scope ) );
        }

        protected Opt<Scoped> _context( Class<?> type, String scope ) {
            return _local( type, scope ).or( () -> {
                LOG.info( "Context: not found locally: %s (%s) %s", type.getName(), scope, clientPage );
                return parent != null ? parent._context( type, scope ) : Opt.absent();
            });
        }

        @Override
        public <R> R context( Class<R> type, String scope ) {
            return type.cast( _context( type, scope ).map( scoped -> scoped.value ).orNull() );
        }
    }

    // instance *******************************************

    private UIComposite         rootContainer;

    private Deque<PageHolder>   pages = new ArrayDeque<>();


    protected Pageflow( UIComposite rootContainer ) {
        this.rootContainer = Assert.notNull( rootContainer, "rootContainer must not be null" );
        this.rootContainer.layout.set( new PageStackLayout() );
        //new PageCloseGesture( rootContainer );
    }


    /**
     * Prepare a {@link PageBuilder} in order to open a new page.
     *
     * @param page An instance of {@link Page} or an annotated object that acts as
     *        the controller of the newly created page.
     */
    public PageBuilder create( Object page ) {
        return new PageBuilder( page );
    }


    /**
     * API for client code to create/open a new Page.
     */
    public class PageBuilder
            extends PageHolder {

        protected Position  origin;

        public PageBuilder( Object page ) {
            if (page instanceof Page) {
                this.clientPage = this.page = (Page)page;
            }
            else {
                this.clientPage = page;
                this.page = new AnnotatedPage( page, Pageflow.this );
            }
            // default context
            putContext( PageBuilder.this, Page.Context.DEFAULT_SCOPE ); // PageSite
            if (pages.isEmpty()) {
                putContext( Pageflow.this, Page.Context.DEFAULT_SCOPE );
            }
        }

        public PageBuilder origin( @SuppressWarnings("hiding") Position origin ) {
            this.origin = origin;
            return this;
        }

        public PageBuilder parent( @SuppressWarnings("hiding") Object parent ) {
            Assert.that( pages.isEmpty() || parent == pages.peek().clientPage, "Adding other than top page is not supported yet." );
            this.parent = pages.peek();
            return this;
        }

        public PageBuilder putContext( Object value, String scope ) {
            var scoped = _local( value.getClass(), scope ).orElse( () -> {
                var newEntry = new Scoped( value, scope );
                context.add( newEntry );
                //LOG.info( "putContext: %s (%s)\n        %s", value.toString(), scope, this );
                return newEntry;
            });
            scoped.value = value;
            Assert.that( _local( value.getClass(), scope ).isPresent() );
            return this;
        }

        /**
         * Actually opens the newly created page in the UI.
         */
        public void open() {
            if (page instanceof AnnotatedPage) {
                ((AnnotatedPage)page).inject( (type,scope) -> context( type, scope ) );
            }
            pages.push( this );
            page.init( this );
            ui = page.createUI( rootContainer );

            var layout = (PageStackLayout)rootContainer.layout.value();
            layout.layout( rootContainer ); // do NOT layout ALL child components

            if (ui instanceof UIComposite) {
                ((UIComposite)ui).layout();
            }
            layout.openLast( origin );
        }
    }


    public void close( Object page ) {
        Assert.isSame( page, pages.peek().clientPage, "Removing other than top page is not supported yet." );
        var pageData = pages.pop();
        pageData.ui.cssClasses.add( "Closing" );
        with( pageData.ui.position ).apply( pos -> pos.set(
                Position.of( pos.value().x, rootContainer.clientSize.value().height() - 30 ) ) );

        Platform.schedule( 750, () -> {
            var closing = pageData.page.close();
            Assert.isEqual( true, closing, "Vetoing Page.close() is not yet supported." );
            pageData.page.dispose();
            if (!pageData.ui.isDisposed()) {
                pageData.ui.dispose();
            }
            // rootContainer.layout();
        });
    }


    /**
     * The sequence of pages in this Pageflow.
     */
    public Sequence<Object,RuntimeException> pages() {
        return Sequence.of( this.pages ).map( holder -> holder.clientPage );
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
                var top = pages.peek().ui;
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
