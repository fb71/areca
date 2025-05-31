/*
 * Copyright (C) 2021-2025, the @authors. All rights reserved.
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

import static areca.ui.pageflow.PageflowEvent.EventType.PAGE_CLOSED;
import static areca.ui.pageflow.PageflowEvent.EventType.PAGE_CLOSING;
import static areca.ui.pageflow.PageflowEvent.EventType.PAGE_OPENED;
import static areca.ui.pageflow.PageflowEvent.EventType.PAGE_OPENING;

import java.util.ArrayList;
import java.util.List;
import areca.common.Assert;
import areca.common.base.Opt;
import areca.common.base.Sequence;
import areca.common.event.EventListener;
import areca.common.event.EventManager;
import areca.common.event.EventManager.EventHandlerInfo;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Position;
import areca.ui.component2.UIComposite;
import areca.ui.layout.FillLayout;
import areca.ui.pageflow.Page.PageSite;
import areca.ui.pageflow.PageLayout.PageLayoutSite;
import areca.ui.pageflow.PageflowEvent.EventType;

/**
 *
 * @author Falko Bräutigam
 */
class PageflowImpl
        extends Pageflow {

    private static final Log LOG = LogFactory.getLog( PageflowImpl.class );

    protected UIComposite       rootContainer;

    protected PageLayout        layout;

    protected PageStack         pages = new PageStack();


    protected PageflowImpl( UIComposite rootContainer ) {
        EventManager.instance().publish( new PageflowEvent( this, EventType.INITIALIZING ) );
        this.rootContainer = Assert.notNull( rootContainer, "rootContainer must not be null" );

        this.layout = new PageGalleryLayout( new PageLayoutSiteImpl() );
        //this.layout = new PageStackLayout( new PageLayoutSiteImpl() );
        this.rootContainer.layout.set( layout.manager() );

        //new PageCloseGesture( rootContainer );
        EventManager.instance().publish( new PageflowEvent( this, EventType.INITIALIZED ) );
    }


    @Override
    public void dispose() {
        throw new RuntimeException( "not yet implemented." );
    }


    @Override
    public boolean isDisposed() {
        return pages == null;
    }


    protected void pageLifecycle( PageHolder page, EventType type ) {
        Assert.that( page.lifecycle == null || type.ordinal() > page.lifecycle.ordinal() );
        page.lifecycle = type;
        EventManager.instance().publish( new PageflowEvent( this, type, page.page, page.clientPage, page.ui, page ) );
    }


    @Override
    public PageBuilder create( Object page ) {
        var builder = new PageBuilderImpl( page );
        pageLifecycle( builder, PAGE_OPENING );
        return builder;
    }


    protected void open( PageBuilderImpl builder ) {
        var page = builder.page;
        if (page instanceof AnnotatedPage) {
            ((AnnotatedPage)page).inject( (type,scope) -> builder.context( type, scope ) );
        }

        builder.parent().ifPresent( parent -> {
            parent.child().ifPresent( child -> {
                child.close();
            });
        });

        pages.push( builder );
        page.init( builder );

        builder.ui = rootContainer.add( new UIComposite() {{
            layout.set( FillLayout.defaults() );
            cssClasses.add( "PageRoot" );
            // UI is created by PageLayout
        }});
        pageLifecycle( builder, PAGE_OPENED );
    }


    @Override
    public void close( Object clientPage ) {
        close( Sequence.of( pages ).first( p -> p.clientPage == clientPage ).orElseError() );
    }


    protected void close( PageHolder page ) {
        LOG.debug( "close(): %s", page.clientPage.getClass().getName());
        pageLifecycle( page, PAGE_CLOSING );

        page.child().ifPresent( child -> {
            child.close();
            Assert.that( !page.child().isPresent() );
        });

        var closing = page.page.close();
        Assert.that( closing, "Vetoing Page.close() is not yet supported." );
        page.page.dispose();

        pages.pop();
        pageLifecycle( page, PAGE_CLOSED );
    }


    @Override
    public boolean isOpen( Object page ) {
        return Sequence.of( pages )
                .first( holder -> holder.clientPage == page )
                .ifPresentMap( holder -> !holder.isClosed() )
                .orElse( false );
    }

    @Override
    public Sequence<Object,RuntimeException> pages() {
        return Sequence.of( pages ).map( holder -> holder.clientPage );
    }

    @Override
    public Object topPage() {
        return pages.peek().clientPage;
    }

    /**
     *
     */
    protected class PageHolder
            extends PageSite {

        /** The root component of the UI of this {@link Page} */
        UIComposite     ui;

        /** Page supplied by client code or {@link AnnotatedPage}. */
        Page            page;

        /** The page impl supplied by client code, Pojo or {@link Page}. */
        Object          clientPage;

        int             pageIndex = pages.size();

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
                    .performIf( PageflowEvent.class, ev -> ev.type == type && ev.clientPage == clientPage )
                    .unsubscribeIf( () -> isClosed() );
        }

        @Override
        public PageBuilder createPage( Object newPage ) {
            return PageflowImpl.this.create( newPage ).parent( clientPage );
        }

        @Override
        public void close() {
            PageflowImpl.this.close( this );
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
                return parent().map( parent -> parent._context( type, scope ).orNull() );
            });
        }

        @Override
        public <R> R context( Class<R> type, String scope ) {
            return type.cast( _context( type, scope ).map( scoped -> scoped.value ).orNull() );
        }

        protected Opt<PageHolder> parent() {
            return pages.at( pageIndex - 1 );
        }

        protected Opt<PageHolder> child() {
            return pages.at( pageIndex + 1 );
        }
    }


    /**
     * The {@link PageBuilder} aspect of an {@link PageHolder}.
     */
    protected class PageBuilderImpl
            extends PageHolder
            implements PageBuilder {

        protected Position  origin;

        public PageBuilderImpl( Object page ) {
            if (page instanceof Page) {
                this.clientPage = this.page = (Page)page;
            }
            else {
                this.clientPage = page;
                this.page = new AnnotatedPage( page, PageflowImpl.this );
            }
            // default context
            putContext( PageBuilderImpl.this, Page.Context.DEFAULT_SCOPE ); // PageSite
            if (pages.isEmpty()) {
                putContext( PageflowImpl.this, Page.Context.DEFAULT_SCOPE );
            }
        }

        @Override
        public PageBuilder origin( @SuppressWarnings("hiding") Position origin ) {
            this.origin = origin;
            return this;
        }

        @Override
        public PageBuilder parent( Object parent ) {
            pageIndex = Sequence.of( pages )
                    .first( p -> p.clientPage == parent )
                    .map( p -> p.pageIndex + 1 ).orElseError();
            LOG.warn( "parent(): pageIndex=%s (%s)", pageIndex, parent.getClass().getName() );
            return this;
        }

        @Override
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

        @Override
        public void open() {
            PageflowImpl.this.open( this );
        }
    }


    /**
     *
     */
    protected class PageLayoutSiteImpl
            implements PageLayoutSite {

        @Override
        public Opt<PageHolder> page( UIComposite pageContainer ) {
            return Sequence.of( pages ).first( holder -> holder.ui == pageContainer );
        }

        @Override
        public Pageflow pageflow() {
            return PageflowImpl.this;
        }

        @Override
        public UIComposite container() {
            return rootContainer;
        }
    }

    /**
     *
     */
    protected class PageStack
            extends ArrayList<PageHolder> {

        private int top = -1;

        public void push( PageHolder elm ) {
            add( ++top, elm );
        }

        public PageHolder pop() {
            return remove( top-- );
        }

        public PageHolder peek() {
            return get( top );
        }

        public Opt<PageHolder> at( int index ) {
            return Opt.of( index >= 0 && index < size() ? get( index ) : null );
        }
    }
}
