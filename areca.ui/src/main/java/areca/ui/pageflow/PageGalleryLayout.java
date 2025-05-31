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

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import areca.common.Assert;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Position;
import areca.ui.Size;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComponent.CssStyle;
import areca.ui.component2.UIComposite;

/**
 *
 * @author Falko Bräutigam
 */
public class PageGalleryLayout
        extends PageStackLayout
        implements PageLayout {

    private static final Log LOG = LogFactory.getLog( PageGalleryLayout.class );

    private static final String CSS_PAGE_RESTING = "PageResting";

    public static final int SPACE = 8;

    // instance *******************************************

    protected Map<UIComponent,Pair<Size,Position>> computed;


    public PageGalleryLayout( PageLayoutSite site ) {
        super( site );
        site.container().styles.add( CssStyle.of( "perspective", "6000px" ) );
    }


    @Override
    public void layout( UIComposite composite ) {
        Assert.isSame( composite, site.container() );

        // simple stack layout on phone screen
        if (composite.size.$().height() > composite.size.$().width() ) {
            super.layout( composite );
            return;
        }

        composite.clientSize.opt().ifPresent( size -> {
            var visiblePages = new PositionSizeMap();
            var availWidth = size.width(); // available width

            // closing Pages are left alone
            var components = composite.components.values()
                    .filter( c -> site.page( (UIComposite)c ).isPresent() )
                    .toList();

            // find visible pages and widths
            var dialogs = new ArrayList<UIComponent>();
            for (int i = components.size() - 1; i >= 0; i--) {
                var page = site.page( (UIComposite)components.get( i ) ).orElseError();

                // dialog
                if (page.isDialog.opt().orElse( false )) {
                    dialogs.add( components.get( i ) );
                    continue;
                }
                var w = page.prefWidth.opt().orElseError();
                if (w > availWidth) {
                    w = i == components.size() - 1
                            ? availWidth  // top page
                            : page.minWidth.opt().orElseError();
                }
                if (w > availWidth) {
                    break;
                }
                var x = availWidth - w;
                visiblePages.put( components.get( i ), x, w );

                // give all dialogs above us our size
                for (var dialog : dialogs) {
                    visiblePages.put( dialog, x, w );
                }
                dialogs.clear();

                availWidth -= w + SPACE;
            }

            // margin
            var margin = (availWidth + SPACE) / 2;
            if (visiblePages.size() < components.size()) { // some space for resting pages on the left
                margin = Math.max( margin - 50, 0 );
            }

            for (var component : visiblePages.keySet()) {
                component.position.set( Position.of( visiblePages.positionOf( component ) - margin, 0 ) );

                var currentWidth = component.size.opt().map( Size::width ).orElse( 0 );
                var width = visiblePages.sizeOf( component );
                component.size.set( Size.of( width, size.height() ) );

                if (width != currentWidth) {
                    ((UIComposite)component).layout();
                }
                component.styles.remove( CssStyle.of( "transform", "" ) );
                component.cssClasses.remove( CSS_PAGE_RESTING );
            }

            // resting pages
            var index = 0;
            for (var component : components) {
                if (!visiblePages.containsKey( component ) ) {
                    component.position.set( Position.of( (index++ * 50) + 50, 0 ) );

                    var translateX = component.size.$().width() / 2;
                    component.styles.add( CssStyle.of( "transform",
                            format( "translateX(-%spx) rotate3d(0,1,0,88deg) scale(0.80)", translateX ) ) );
                    component.cssClasses.add( CSS_PAGE_RESTING );
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

}
