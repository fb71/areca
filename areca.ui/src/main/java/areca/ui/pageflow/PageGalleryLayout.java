/*
 * Copyright (C) 2025, the @authors. All rights reserved.
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

import static areca.ui.pageflow.PageflowEvent.EventType.PAGE_CLOSING;
import static areca.ui.pageflow.PageflowEvent.EventType.PAGE_OPENED;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import areca.common.Assert;
import areca.common.Platform;
import areca.common.event.EventCollector;
import areca.common.event.EventManager;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Position;
import areca.ui.Size;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComponent.CssStyle;
import areca.ui.component2.UIComposite;
import areca.ui.layout.AbsoluteLayout;
import areca.ui.layout.LayoutManager;
import areca.ui.pageflow.PageflowImpl.PageHolder;

/**
 *
 * @author Falko Bräutigam
 */
class PageGalleryLayout
        extends AbsoluteLayout
        implements PageLayout {

    private static final Log LOG = LogFactory.getLog( PageGalleryLayout.class );

    public static final int DEFAULT_PAGE_WIDTH = 500;

    public static final int DEFAULT_PAGE_WIDTH_MIN = 300;

    public static final int SPACE = 8;

    protected Pageflow          pageflow;

    protected PageLayoutSite    site;

    protected Map<UIComponent,Pair<Size,Position>> computed;


    public PageGalleryLayout( PageLayoutSite site ) {
        this.site = site;
        var collector = new EventCollector<PageflowEvent>( 1 );
        EventManager.instance()
                .subscribe( (PageflowEvent ev) -> {
                    // open
                    if (ev.type == PAGE_OPENED) {
                        LOG.debug( "PageflowEvent: %s (%s)", ev.type, ev.clientPage.getClass().getSimpleName() );
                        ev.pageUI.cssClasses.add( "PageOpening" );
                        // give the new page its size so that PageOpening animation works
                        layout( site.container() );
                    }
                    // close
                    else if (ev.type == PAGE_CLOSING) {
                        LOG.debug( "PageflowEvent: %s (%s)", ev.type, ev.clientPage.getClass().getSimpleName() );
                        ev.pageUI.cssClasses.add( "PageClosing" );
                        ev.pageUI.position.set( ev.pageUI.position.$().add( 0, site.container().clientSize.value().height() / 4 ) );
                    }

                    // deferred layout
                    collector.collect( ev, events -> {
                        LOG.debug( "Layout: (%s) : %s", site.container().components.size(),
                                site.pageflow().pages().reduce( "", (r,p) -> r + p.getClass().getSimpleName() + ", " ) );

                        for (var _ev : events) {
                            // opened
                            if (_ev.type == PAGE_OPENED) {
                                _ev.pageUI.styles.add( CssStyle.of( "transition-delay", Platform.isJVM() ? "0.15s" : "0.2s" ) );
                                _ev.pageUI.cssClasses.remove( "PageOpening" );

                                // createUI() *after* PageRoot composite is rendered with PageOpening CSS
                                // class to make sure that Page animation starts after given delay no matter
                                // what the createUI() method does
                                _ev.page.createUI( _ev.pageUI );

                                Platform.schedule( 1000, () -> {
                                    _ev.pageUI.styles.remove( CssStyle.of( "transition-delay", "0.2s") );
                                });
                            }
                            // closed
                            else if (_ev.type == PAGE_CLOSING) {
                                Platform.schedule( 500, () -> { // time PageClosing animation
                                    if (!_ev.pageUI.isDisposed()) {
                                        _ev.pageUI.dispose();
                                    }
                                });
                            }
                        }
                        layout(); // FIXME layout just the size changed pages
                    });
                })
                .performIf( PageflowEvent.class, ev -> ev.getSource() == site.pageflow() )
                .unsubscribeIf( () -> site.pageflow().isDisposed() );
    }


    protected void layout() {
        layout( site.container() );

        // XXX nur die, die sich geändert haben
        for (var child : site.container().components.value()) {
            if (child instanceof UIComposite ) {
                ((UIComposite)child).layout();
            }
        }
    }


    @Override
    public void layout( UIComposite composite ) {
        Assert.isSame( composite, site.container() );
        super.layout( composite );

        composite.clientSize.opt().ifPresent( size -> {
            var result = new PositionSizeMap();
            var availWidth = size.width(); // available width

            // find visible pages and widths
            var components = composite.components.values().toList();
            for (int i = components.size() - 1; i >= 0; i--) {
                var page = site.page( (UIComposite)components.get( i ) ).orElse( (PageHolder)null );
                if (page == null) {  // closing Pages are left alone
                    continue;
                }
                var w = page.prefWidth.opt().orElse( DEFAULT_PAGE_WIDTH );
                if (w > availWidth) {
                    w = i == components.size() - 1
                            ? availWidth  // top page
                            : page.minWidth.opt().orElse( DEFAULT_PAGE_WIDTH_MIN );
                }
                if (w > availWidth) {
                    break;
                }
                result.put( components.get( i ), availWidth - w, w );
                availWidth -= w + SPACE;
            }
            // margin
            var margin = (availWidth + SPACE) / 2;
            for (var component : result.keySet()) {
                component.position.set( Position.of( result.positionOf( component ) - margin, 0 ) );

                var currentWidth = component.size.opt().map( Size::width ).orElse( 0 );
                var width = result.sizeOf( component );
                component.size.set( Size.of( width, size.height() ) );
                //component.opacity.set( 1f );

                if (width != currentWidth) {
                    ((UIComposite)component).layout();
                }
            }

            // hide invisible
            for (var component : composite.components.$()) {
                if (!result.containsKey( component )) {
                    //component.opacity.set( 0f );
                }
            }
        });
    }

    /**
     *
     */
    protected static class PositionSizeMap
            extends HashMap<UIComponent, Pair<Integer,Integer>> {

        public void put( UIComponent c, Integer x, Integer width ) {
            put( c, Pair.of( x, width ) );
        }

        public int sizeOf( UIComponent c ) {
            return get( c ).getRight();
        }

        public int positionOf( UIComponent c ) {
            return get( c ).getLeft();
        }
    }

    @Override
    public LayoutManager manager() {
        return this;
    }

}
