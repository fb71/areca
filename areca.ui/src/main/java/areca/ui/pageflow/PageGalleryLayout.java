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

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Position;
import areca.ui.Size;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.layout.AbsoluteLayout;
import areca.ui.pageflow.Pageflow.PageLayoutSite;

/**
 *
 * @author Falko Bräutigam
 */
public class PageGalleryLayout
        extends AbsoluteLayout {

    private static final Log LOG = LogFactory.getLog( PageGalleryLayout.class );

    public static final int DEFAULT_PAGE_WIDTH = 500;

    public static final int DEFAULT_PAGE_WIDTH_MIN = 300;

    public static final int SPACE = 5;

    protected Pageflow      pageflow;

    private UIComposite     composite;

    private PageLayoutSite  site;


    public PageGalleryLayout( PageLayoutSite site ) {
        this.site = site;
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

}
