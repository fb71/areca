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
import static org.apache.commons.lang3.time.DateUtils.addDays;
import static org.apache.commons.lang3.time.DateUtils.addYears;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.apache.commons.lang3.time.DateUtils;

import org.polymap.model2.query.Query.Order;
import org.polymap.model2.runtime.Lifecycle.State;

import areca.app.ArecaApp;
import areca.app.model.Anchor;
import areca.app.model.ModelSubmittedEvent;
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
        this.site = page.site();
        this.page = page;

        this.body = body
            .layout.set( new FillLayout() )
            .add( new ScrollableComposite() );
        createBody();
    }


    @Override
    protected UIComponent doInit( UIComposite parent ) {
        var ui = new PageContainer( this, parent );
        ui.title.set( "Anchors" );

        body = ui.body;
        createBody();
        return ui;
    }


    protected void createBody() {
        // XXX wait for repo to show up
        if (ArecaApp.instance().repo() == null) {
            LOG.info( "waiting for repo..." );
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
                                            var btn = btns.computeIfAbsent( anchor, __ -> makeAnchorButton( anchor ) );
                                            return new CloudComponent( btn, i + start, anchor.lastMessageDate.get() );
                                        })
                                        .toList();
                            }));
        }});
        body.layout();

        // listen to model updates
        EventManager.instance()
                .subscribe( (ModelSubmittedEvent ev) -> body.layout())
                .performIf( ModelSubmittedEvent.class::isInstance )
                .unsubscribeIf( () -> body.isDisposed() );
    }


    protected Button makeAnchorButton( Anchor anchor ) {
        var btn = new Button();
        btn.cssClasses.add( "AnchorButton" );

        // btn
        Runnable updateBtn = () -> {
            btn.label.set( abbreviate( anchor.name.opt().orElse( "..." ), 17 ) );
            btn.tooltip.set( anchor.name.opt().orElse( "" ) );
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
        var tag = new Tag( btn );
        if (anchor.storeRef.get().startsWith( "contact:" )) {
            tag.icons.add( "face" );
        }
        else if (anchor.storeRef.get().startsWith( "pseudo-contact:" )) {
            tag.icons.add( "alternate_email" );
        }
        else if (anchor.storeRef.get().startsWith( "imap-folder:" )) {
            tag.icons.add( "folder" );
        }
        else if (anchor.storeRef.get().startsWith( "matrix-room:" )) {
            tag.icons.add( "3p" );
        }
        Runnable updateTag = () -> {
            anchor.unreadMessagesCount().onSuccess( unread -> {
                if (unread > 0) {
                    btn.cssClasses.add( "HasUnreadMessages" );
                } else {
                    btn.cssClasses.remove( "HasUnreadMessages" );
                }
            });
        };
        Platform.schedule( 1250, updateTag );

        // check updates
        anchor.onLifecycle( State.AFTER_REFRESH, ev -> {
            updateBtn.run();
            //updateBadge.run();
            updateTag.run();
        })
        .unsubscribeIf( () -> btn.isDisposed() );

        // click
        btn.events.on( EventType.SELECT, ev -> {
            site.put( anchor );
            site.pageflow().open( new MessagesPage( anchor.messages, anchor.name.get() ), page, ev.clientPos() );
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

        protected List<IntervalBorder>  intervalBorders = new ArrayList<>();

        protected Interval              currentInterval;


        public CloudRaster() {
            var today = DateUtils.truncate( new Date(), Calendar.DATE );
            today = DateUtils.addHours( today, -12 ); // ???
            intervalBorders.add( new IntervalBorder( "Today", today.getTime() ) );
            intervalBorders.add( new IntervalBorder( "Yesterday", addDays( today, -1 ).getTime() ) );
            intervalBorders.add( new IntervalBorder( "2 days ago", addDays( today, -2 ).getTime() ) );
            intervalBorders.add( new IntervalBorder( "3 days ago", addDays( today, -3 ).getTime() ) );
            intervalBorders.add( new IntervalBorder( "7 days ago", addDays( today, -7 ).getTime() ) );
            intervalBorders.add( new IntervalBorder( "2 weeks ago", addDays( today, -14 ).getTime() ) );
            intervalBorders.add( new IntervalBorder( "1 month ago", addDays( today, -30 ).getTime() ) );
            intervalBorders.add( new IntervalBorder( "Older", addYears( today, -30 ).getTime() ) );
            LOG.info( "Intervals: %s", Sequence.of( intervalBorders ).map( b -> df.format( b.start ) ) );
        }


        @Override
        public void componentHasChanged( Component changed ) {
            throw new RuntimeException( "not yet implemented." );
        }


        @Override
        public void layout( UIComposite composite ) {
            LOG.info( "layout(): clientSize=%s", composite.clientSize.opt().orElse( Size.of( -1, -1 ) ) );
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

            scrollable.scrollTop.onChange( (newValue,__) -> {
                checkScroll();
            });
            checkScroll();
        }


        protected void checkScroll() {
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

            if (lines.isEmpty() || (lines.getLast().bottom() - (viewSize.height()/2)) < (viewTop + viewSize.height())) {
                if (currentInterval.isComplete) {
                    LOG.info( "Raster: interval: isComplete: %s", currentInterval.isComplete );
                    currentInterval = currentInterval.next();
                    checkVisibleLines();
                }
                else {
                    new RasterLine().layout().onSuccess( hasMore -> {
                        if (hasMore) {
                            checkVisibleLines();
                        }
                    });
                }
            }
        }

        protected RasterLine lastLine() {
            return lines.isEmpty() ? null : lines.getLast();
        }


        /**
         * A line in the {@link CloudRaster}.
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
                    LOG.info( "%s: loaded: top=%s, startIndex=%s, components=%d (%s)", getClass().getSimpleName(),
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

                    new Label( this ).content.set( label );
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
        protected static class IntervalBorder {
            public String           label;
            public long             start; // time

            public IntervalBorder( String label, long start ) {
                this.label = label;
                this.start = start;
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
                LOG.info( "Interval: count=%d, component.date=%s, borderIndex=%d",componentCount, df.format( component.date ), borderIndex );
                var border = intervalBorders.get( borderIndex );

                // find border on first component
                if (componentCount == 0) {
                    while (component.date < border.start) {
                        border = intervalBorders.get( ++borderIndex );
                    }
                    LOG.info( "    borderIndex=%d", borderIndex );
                    lines.add( new SeparatorLine( intervalBorders.get( borderIndex ).label ) );
                }

                if (component.date >= border.start) {
                    componentCount ++;
                }
                else {
                    LOG.info( "    isComplete!" );
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
