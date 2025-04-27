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

import java.util.HashMap;
import org.apache.commons.lang3.tuple.Pair;

import areca.common.Platform;
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
import areca.ui.pageflow.PageflowEvent.EventType;

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

    public static final int SPACE = 5;

    protected Pageflow      pageflow;

    private UIComposite     composite;

    private PageLayoutSite  site;


    public PageGalleryLayout( PageLayoutSite site ) {
        this.site = site;
        EventManager.instance()
                .subscribe( (PageflowEvent ev) -> {
                    if (ev.type == EventType.PAGE_OPENED) {
                        pageOpened( ev );
                    }
                    else if (ev.type == EventType.PAGE_CLOSING) {
                        pageClosing( ev );
                    }
                })
                .performIf( PageflowEvent.class, ev -> ev.getSource() == site.pageflow() )
                .unsubscribeIf( () -> site.pageflow().isDisposed() );
    }


    protected void pageOpened( PageflowEvent ev ) {
        ev.pageUI.cssClasses.add( "PageOpening" );

        // createUI() *after* PageRoot composite is rendered with PageOpening CSS
        // class to make sure that Page animation starts after given delay no matter
        // what the createUI() method does
        Platform.schedule( 1, () -> {
            ev.pageUI.styles.add( CssStyle.of( "transition-delay", Platform.isJVM() ? "0.15s" : "0.2s" ) );
            ev.pageUI.cssClasses.remove( "PageOpening" );

            ev.page.createUI( ev.pageUI );
            ev.pageUI.layout();

            Platform.schedule( 1000, () -> {
                ev.pageUI.styles.remove( CssStyle.of( "transition-delay", "0.2s") );
            });
        });
    }


    protected void pageClosing( PageflowEvent ev ) {
        ev.pageUI.cssClasses.add( "PageClosing" );
    }


    @Override
    public void layout( @SuppressWarnings("hiding") UIComposite composite ) {
        super.layout( composite );
        this.composite = composite;

        composite.clientSize.opt().ifPresent( size -> {
            var result = new PositionSizeMap();
            var x = size.width();

            // find visible pages and widths
            var components = composite.components.values().toList();
            for (int i = components.size() - 1; i >= 0; i--) {

                var page = site.page( (UIComposite)components.get( i ) );
                var w = page.prefWidth.opt().orElse( DEFAULT_PAGE_WIDTH );
                if (x < w) {
                    w = page.minWidth.opt().orElse( DEFAULT_PAGE_WIDTH_MIN );
                }
                if (x < w) {
                    break;
                }
                result.put( components.get( i ), x - w, w );
                x -= w + SPACE;
            }
            // margin
            var margin = (x + SPACE) / 2;
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
