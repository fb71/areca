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
import static areca.ui.pageflow.PageflowEvent.EventType.PAGE_CLOSED;
import static areca.ui.pageflow.PageflowEvent.EventType.PAGE_CLOSING;
import static areca.ui.pageflow.PageflowEvent.EventType.PAGE_OPENED;
import static areca.ui.pageflow.PageflowEvent.EventType.PAGE_OPENING;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import areca.common.Assert;
import areca.common.Platform;
import areca.common.Session;
import areca.common.base.Opt;
import areca.common.base.Sequence;
import areca.common.event.EventListener;
import areca.common.event.EventManager;
import areca.common.event.EventManager.EventHandlerInfo;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Position;
import areca.ui.component2.UIComponent.CssStyle;
import areca.ui.component2.UIComposite;
import areca.ui.gesture.PanGesture;
import areca.ui.layout.FillLayout;
import areca.ui.pageflow.Page.PageSite;
import areca.ui.pageflow.PageflowEvent.EventType;

/**
 *
 * @author Falko Bräutigam
 */
public class Pageflow {

    private static final Log LOG = LogFactory.getLog( Pageflow.class );

    public static Pageflow start( UIComposite rootContainer ) {
        return Session.setInstance( new Pageflow( rootContainer ) );
    }

    public static Pageflow current() {
        return Session.instanceOf( Pageflow.class );
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
        UIComposite     ui;
        /** Page supplied by client code or {@link AnnotatedPage}. */
        Page            page;
        /** The page impl supplied by client code, Pojo or {@link Page}. */
        Object          clientPage;

        PageHolder      parent;

        List<Scoped>    context = new ArrayList<>();

        /** The last thrown {@link EventType} for this page. */
        EventType       lifecycle;


        @Override
        public String toString() {
            return String.format( "PageSite[page=%s, context=[%s]]", clientPage.getClass().getSimpleName(),
                    Sequence.of( context )
                            .map( scoped -> scoped.value )
                            .map( value -> value instanceof PageSite ? "[PageSite]" : value.toString() )
                            .reduce( (r,elm) -> r + ", " ).orElse( "" ) );
        }

        @Override
        public EventHandlerInfo subscribe( EventType type, EventListener<PageflowEvent> l ) {
            return EventManager.instance()
                    .subscribe( l )
                    .performIf( PageflowEvent.class, ev -> ev.type == type && ev.page.get() == clientPage )
                    .unsubscribeIf( () -> isClosed() );
        }

        @Override
        public PageBuilder createPage( Object newPage ) {
            return Pageflow.this.create( newPage ).parent( clientPage );
        }

        @Override
        public void close() {
            Pageflow.this.close( clientPage );
        }

        @Override
        public boolean isClosed() {
            return lifecycle.ordinal() >= EventType.PAGE_CLOSING.ordinal();
        }

        protected Opt<Scoped> _local( Class<?> type, String scope ) {
            return Sequence.of( context ).first( scoped -> scoped.isCompatible( type, scope ) );
        }

        protected Opt<Scoped> _context( Class<?> type, String scope ) {
            return _local( type, scope ).or( () -> {
                LOG.debug( "Context: not found locally: %s (%s) %s", type.getName(), scope, clientPage );
                return parent != null ? parent._context( type, scope ) : Opt.absent();
            });
        }

        @Override
        public <R> R context( Class<R> type, String scope ) {
            return type.cast( _context( type, scope ).map( scoped -> scoped.value ).orNull() );
        }
    }

    /**
     *
     */
    class PageLayoutSite {

        public PageHolder page( UIComposite pageContainer ) {
            return Sequence.of( pages ).first( holder -> holder.ui == pageContainer ).orElseError();
        }

    }

    // instance *******************************************

    private UIComposite         rootContainer;

    private Deque<PageHolder>   pages = new ArrayDeque<>();


    protected Pageflow( UIComposite rootContainer ) {
        EventManager.instance().publish( new PageflowEvent( this, null, EventType.INITIALIZING ) );
        this.rootContainer = Assert.notNull( rootContainer, "rootContainer must not be null" );

        //this.rootContainer.layout.set( new PageStackLayout() );
        this.rootContainer.layout.set( new PageGalleryLayout( new PageLayoutSite() ) );

        //new PageCloseGesture( rootContainer );
        EventManager.instance().publish( new PageflowEvent( this, null, EventType.INITIALIZING ) );
    }


    public void dispose() {
        throw new RuntimeException( "not yet implemented." );
    }


    public boolean isDisposed() {
        return pages == null;
    }


    protected void pageLifecycle( PageHolder page, EventType type ) {
        Assert.that( page.lifecycle == null || type.ordinal() > page.lifecycle.ordinal() );
        page.lifecycle = type;
        EventManager.instance().publish( new PageflowEvent( this, Assert.notNull( page ).clientPage, type ) );
    }


    /**
     * Prepare a {@link PageBuilder} in order to open a new page.
     *
     * @param page An instance of {@link Page} or an annotated object that acts as
     *        the controller of the newly created page.
     */
    public PageBuilder create( Object page ) {
        var result = new PageBuilder( page );
        pageLifecycle( result, PAGE_OPENING );
        return result;
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
            //Assert.that( pages.isEmpty() || parent == pages.peek().clientPage, "Adding other than top page is not supported yet." );
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

            ui = rootContainer.add( new UIComposite() {{
                layout.set( FillLayout.defaults() );
                cssClasses.add( "PageRoot" );
                cssClasses.add( "PageOpening" );
            }});
            var layout = rootContainer.layout.value();
            layout.layout( rootContainer ); // don't recursivly layout ALL child components

            // createUI() *after* PageRoot composite is rendered with PageOpening class
            // to make sure that Page animation starts after given delay
            // no matter what the createUI() method does
            Platform.schedule( 1, () -> {
                ui.styles.add( CssStyle.of( "transition-delay", Platform.isJVM() ? "0.15s" : "0.2s" ) );
                ui.cssClasses.remove( "PageOpening" );

                page.createUI( ui );
                ui.layout();

                Platform.schedule( 1000, () -> {
                    ui.styles.remove( CssStyle.of( "transition-delay", "0.2s") );
                });
            });
            pageLifecycle( this, PAGE_OPENED );
        }
    }


    public void close( Object page ) {
        Assert.isSame( page, pages.peek().clientPage, "Removing other than top page is not supported yet." );
        var pageData = pages.pop();
        pageLifecycle( pageData, PAGE_CLOSING );
        pageData.ui.cssClasses.add( "PageClosing" );
        with( pageData.ui.position ).apply( pos -> pos.set(
                Position.of( pos.value().x, rootContainer.clientSize.value().height() - 30 ) ) );

//        Platform.schedule( 750, () -> {
            var closing = pageData.page.close();
            Assert.isEqual( true, closing, "Vetoing Page.close() is not yet supported." );
            pageData.page.dispose();
            if (!pageData.ui.isDisposed()) {
                pageData.ui.dispose();
            }
            pageLifecycle( pageData, PAGE_CLOSED );

            var layout = rootContainer.layout.value();
            layout.layout( rootContainer ); // don't recursivly layout ALL child components
//        });
    }


    /**
     * True if the given page currently exists and is open.
     */
    public boolean isOpen( Object page ) {
        return Sequence.of( pages )
                .first( holder -> holder.clientPage == page )
                .ifPresentMap( holder -> !holder.isClosed() )
                .orElse( false );
    }


    /**
     * The sequence of pages in this Pageflow.
     */
    public Sequence<Object,RuntimeException> pages() {
        return Sequence.of( pages ).map( holder -> holder.clientPage );
    }


    public Object topPage() {
        return pages.peek().clientPage;
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
