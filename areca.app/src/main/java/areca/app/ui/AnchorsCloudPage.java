/*
 * Copyright (C) 2022, the @authors. All rights reserved.
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
package areca.app.ui;

import static org.apache.commons.lang3.StringUtils.abbreviate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.polymap.model2.query.Query.Order;
import org.polymap.model2.runtime.EntityRuntimeContext.EntityStatus;

import areca.app.ArecaApp;
import areca.app.model.Anchor;
import areca.app.model.ModelUpdateEvent;
import areca.app.service.ContactAnchorSynchronizer;
import areca.app.service.mail.MailFolderSynchronizer.FolderAnchorStoreRef;
import areca.app.service.mail.PseudoContactSynchronizer;
import areca.app.service.matrix.MatrixService;
import areca.app.ui.AnchorsCloudPage.CloudRaster.CloudComponent;
import areca.common.Platform;
import areca.common.Promise;
import areca.common.Timer;
import areca.common.base.Sequence;
import areca.common.event.EventManager;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Position;
import areca.ui.Size;
import areca.ui.component2.Button;
import areca.ui.component2.Events.EventType;
import areca.ui.component2.Label;
import areca.ui.component2.ScrollableComposite;
import areca.ui.component2.Separator;
import areca.ui.component2.Tag;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.layout.DynamicLayoutManager;
import areca.ui.layout.FillLayout;
import areca.ui.pageflow.Page;
import areca.ui.pageflow.PageContainer;
import areca.ui.pageflow.Pageflow;

/**
 *
 * @author Falko Bräutigam
 */
public class AnchorsCloudPage
        extends Page {

    private static final Log LOG = LogFactory.getLog( AnchorsCloudPage.class );

    protected UIComposite     body;

    protected StartPage       page;

    protected Map<Anchor,Button> btns = new HashMap<>();


    /** Work from within StartPage */
    public AnchorsCloudPage( UIComposite body, StartPage page ) {
        this.pageSite = page.site();
        this.page = page;

        this.body = body
            .layout.set( new FillLayout() )
            .add( new ScrollableComposite() );
        createBody();
    }


    @Override
    protected UIComponent onCreateUI( UIComposite parent ) {
        var ui = new PageContainer( this, parent );
        ui.title.set( "Anchors" );

        body = ui.body;
        createBody();
        return ui;
    }


    private int reentryCount = 0;

    private Set<Anchor> set;

    protected void createBody() {
        // XXX wait for repo to show up
        if (ArecaApp.instance().repo() == null) {
            LOG.info( "waiting for repo..." );
            reentryCount ++;
            Platform.schedule( 100, () -> createBody() );
            return;
        }
        body.layout.set( new CloudRaster() {{
                provider.set( (start,num) ->
                        ArecaApp.instance().unitOfWork()
                            .query( Anchor.class )
                            .orderBy( Anchor.TYPE.lastMessageDate, Order.DESC )
                            .firstResult( start )
                            .maxResults( num )
                            .executeCollect()
                            .map( anchors -> {
                                return Sequence.of( anchors )
                                        .map( (anchor,i) -> {
                                            var btn = btns.computeIfAbsent( anchor, __ -> createAnchorButton( anchor ) );
                                            return new CloudComponent( btn, i + start, anchor.lastMessageDate.get() );
                                        })
                                        .toList();
                            }));
        }});
        if (reentryCount > 0) {
            body.layout();
        }

        // listen to model updates
        EventManager.instance()
                .subscribe( (ModelUpdateEvent ev) -> {
                    if (!ev.entities( Anchor.class ).isEmpty()) {
                        LOG.info( "Updating layout..." );

                        // remove Buttons of REMOVED anchors
                        Sequence.of( new ArrayList<>( btns.keySet() ) )
                                .filter( anchor -> anchor.status() == EntityStatus.REMOVED )
                                .forEach( anchor -> btns.remove( anchor ).dispose() );

//                        for (var it = btns.entrySet().iterator(); it.hasNext(); ) {
//                            var entry = it.next();
//                            if (entry.getKey().status() == EntityStatus.REMOVED) {
//                                it.remove();
//                                entry.getValue().dispose();
//                                LOG.info( "REMOVED: %s", entry.getKey() );
//                            }
//                        }

                        body.layout();
                    }
                })
                .performIf( ModelUpdateEvent.class::isInstance )
                .unsubscribeIf( () -> body.isDisposed() );
    }


    protected Button createAnchorButton( Anchor anchor ) {
        var btn = new Button();
        btn.cssClasses.add( "AnchorButton" );

        // btn
        Runnable updateBtn = () -> {
            btn.tooltip.set( anchor.name.opt().orElse( "" ) );
            if (anchor.image.opt().isPresent()) {
                btn.bgImage.set( anchor.image.opt().orElse( null ) );
            }
            else {
                btn.label.set( abbreviate( anchor.name.opt().orElse( "..." ), 17 ) );
            }
        };
        updateBtn.run();

//        // badge
//        var badge = new Badge( btn );
//        Runnable updateBadge = () -> {
//            anchor.unreadMessagesCount().onSuccess( unread -> {
//                badge.content.set( unread > 0 ? String.valueOf( unread ) : null );
//            });
//        };

        // tag
        var tag = btn.addDecorator( new Tag() ).get();
        anchor.storeRef( ContactAnchorSynchronizer.ContactAnchorStoreRef.class ).ifPresent( __ -> {
            tag.icons.add( "face" );
        });
        anchor.storeRef( PseudoContactSynchronizer.AnchorStoreRef.class ).ifPresent( __ -> {
            tag.icons.add( "alternate_email" );
        });
        anchor.storeRef( FolderAnchorStoreRef.class ).ifPresent( __ -> {
            tag.icons.add( "folder" );
        });
        anchor.storeRef( MatrixService.RoomAnchorStoreRef.class ).ifPresent( __ -> {
            tag.icons.add( "3p" );
        });
        Runnable updateTag = () -> {
            var unread = anchor.unreadMessagesCount.get();
            btn.cssClasses.modify( "HasUnreadMessages", unread > 0 );
        };
        Platform.schedule( 1250, updateTag );

        // Anchor updates:
        //   - message added on sync
        //   - unread flag of Message changed
        //   - message removed
        EventManager.instance()
                .subscribe( (ModelUpdateEvent modified) -> {
                    if (modified.entities( Anchor.class ).contains( anchor.id() )) {
                        LOG.info( "Anchor modified: %s", anchor.id() );
                        updateBtn.run();
                        updateTag.run();
                    }
                })
                .performIf( ev -> ev instanceof ModelUpdateEvent )
                .unsubscribeIf( () -> btn.isDisposed() );

        // click
        btn.events.on( EventType.SELECT, ev -> {
            Pageflow.current().create( new MessagesPage( anchor, anchor.name.get() ) )
                    .putContext( anchor, Page.Context.DEFAULT_SCOPE )
                    .parent( page )
                    .origin( ev.clientPos() )
                    .open();
        });
        return btn;
    }


    /**
     * {@link #checkVisibleLines()} creates {@link RasterLine}s until currently
     * scrolled view is filled. {@link RasterLine} requests components and does
     * layout according the previous line.
     * <p>
     * The {@link Interval} checks/counts every component requested by a RasterLine
     * in order to create {@link SeparatorLine}s.
     */
    protected static class CloudRaster
            extends DynamicLayoutManager<CloudComponent> {

        protected static final int  spacing = 17;
        protected static final int  margins = 10;
        protected static final int  cWidth = 80;
        protected static final int  cHeight = 75;

        protected static final DateFormat df = SimpleDateFormat.getDateTimeInstance( DateFormat.MEDIUM, DateFormat.MEDIUM );

        protected LinkedList<RasterLine>  lines = new LinkedList<>();

        protected Size                  viewSize;

        protected int                   cols;

        protected Timer                 timer = Timer.start();

        protected Interval              currentInterval;

        protected boolean               isLayouting;


        @Override
        public void componentHasChanged( Component changed ) {
            throw new RuntimeException( "not yet implemented." );
        }


        @Override
        public void layout( UIComposite composite ) {
            LOG.debug( "layout(): clientSize=%s", composite.clientSize.opt().orElse( Size.of( -1, -1 ) ) );
            boolean isFirstRun = scrollable == null;
            super.layout( composite );

            // remove previous lines (separators)
            Sequence.of( lines ).forEach( line -> line.dispose() );
            lines.clear();

            if (composite.clientSize.opt().isAbsent()) {
                return;
            }
            viewSize = composite.clientSize.$(); //.substract( margins ).substract( margins );
            cols = (viewSize.width() - (margins*2) + spacing) / (cWidth + spacing);
            currentInterval = new Interval();

            if (isFirstRun) {
                scrollable.scrollTop.onChange( (newValue,__) -> {
                    checkScroll();
                });
            }
            checkScroll();
        }


        protected void checkScroll() {
            if (isLayouting) {
                LOG.info( "Skipping concurrent layout()" );
                return;
            }
            isLayouting = true;

            LOG.debug( "checkScroll(): scroll=%d, height=%d", scrollable.scrollTop.$(), viewSize.height() );
            timer.restart();
            checkVisibleLines();
        }


        /**
         * Recursivly check if the current last loaded line is "below" the currently
         * scrolled view area. Wait for the current line to be layouted and recursivly
         * check the next line afterwards, so that the next line knows the height of
         * its predecessor.
         */
        protected void checkVisibleLines() {
            //LOG.info( "checkScroll(): scroll=%d, height=%d", scrollable.scrollTop.$(), viewSize.height() );
            int viewTop = scrollable.scrollTop.value();

            if (lines.isEmpty() || (lines.getLast().bottom() - (viewSize.height()/1)) < (viewTop + viewSize.height())) {
                if (currentInterval.isComplete) {
                    LOG.debug( "Raster: interval: isComplete: %s", currentInterval.isComplete );
                    currentInterval = currentInterval.next();
                    checkVisibleLines();
                }
                else {
                    new RasterLine().layout().onSuccess( hasMore -> {
                        if (hasMore) {
                            checkVisibleLines();
                        }
                        else {
                            isLayouting = false;
                        }
                    });
                }
            }
            else {
                isLayouting = false;
            }
        }

        protected RasterLine lastLine() {
            return lines.isEmpty() ? null : lines.getLast();
        }


        /**
         * A line of buttons in the {@link CloudRaster}. It does its layout relative to
         * the {@link CloudRaster#lastLine()}.
         */
        protected class RasterLine {

            public int              startIndex;

            public int              top;

            public int              height;

            public List<CloudComponent> components;

            /**
             * Init 'top' right before the first Button is layouted, so that
             * the {@link Interval} can draw a {@link SeparatorLine} before it.
             */
            public RasterLine() {
                var last = lastLine();
                this.top = last != null ? last.bottom() + spacing : margins;
                this.height = cHeight;
                this.startIndex = last != null ? last.startIndex + last.components.size() : 0;
                //LOG.info( "%s: top=%s, startIndex=%s, num/cols=%d", getClass().getSimpleName(), top, startIndex, cols );
            }

            public void dispose() {
                components.clear();
            }

            public Promise<Boolean> layout() {
                return provider.$().provide( startIndex, cols ).map( loaded -> {
                    LOG.debug( "%s: loaded: top=%s, startIndex=%s, components=%d (%s)", getClass().getSimpleName(),
                            top, startIndex, loaded.size(), timer.elapsedHumanReadable() );

                    this.components = new ArrayList<>( loaded.size() );
                    var left = 0;
                    for (var c : loaded) {
                        if (currentInterval.checkAdd( c )) {
                            if (c.component.parent() == null) {
                                scrollable.add( c.component );
                            }
                            if (components.isEmpty()) {
                                // interval checkAdd() might have inserted a SeparatorLine before us
                                var last = lastLine();
                                this.top = last != null ? last.bottom() + spacing : margins;
                                lines.add( this );
                            }
                            components.add( c );
                            c.component.position.set( Position.of( left + margins, top ) );
                            c.component.size.set( Size.of( cWidth, cHeight ) );
                            left += cWidth + spacing;
                        }
                    }
                    return !loaded.isEmpty();
                });
            }


            public int bottom() {
                return top + height;
            }
        }


        /**
         *
         */
        protected class SeparatorLine
                extends RasterLine {

            public UIComponent      sep;

            public SeparatorLine( String label ) {
                height = 0;
                components = Collections.emptyList();
                sep = scrollable.add( new Separator() {{
                    position.set( Position.of( margins, top ) );
                    size.set( Size.of( viewSize.width() - (margins*2), 1 ) );

                    addDecorator( new Label().content.set( label ) );
                }});
            }

            @Override
            public void dispose() {
                sep.dispose();
            }
        }


        /**
         *
         */
        protected class Interval {
            public boolean  isComplete;
            public int      componentCount;
            public int      borderIndex;

            public boolean checkAdd( CloudComponent component ) {
                LOG.debug( "Interval: count=%d, component.date=%s, borderIndex=%d",componentCount, df.format( component.date ), borderIndex );
                var border = DaySeparatorBorder.borders.get( borderIndex );

                // find border on first component
                if (componentCount == 0) {
                    while (component.date < border.start) {
                        border = DaySeparatorBorder.borders.get( ++borderIndex );
                    }
                    LOG.debug( "    borderIndex=%d", borderIndex );
                    lines.add( new SeparatorLine( DaySeparatorBorder.borders.get( borderIndex ).label ) );
                }

                if (component.date >= border.start) {
                    componentCount ++;
                }
                else {
                    LOG.debug( "    isComplete!" );
                    isComplete = true;
                }
                return !isComplete;
            }

            public Interval next() {
                var result = new Interval();
                result.borderIndex = this.borderIndex;
                return result;
            }
        }


        /**
         * Adds a date to a {@link Component}.
         */
        public static class CloudComponent
                extends DynamicLayoutManager.Component  {

            public long         date;

            public CloudComponent( UIComponent component, int index, long date ) {
                super( component, index );
                this.date = date;
            }
        }
    }
}
