/*
 * Copyright (C) 2023, the @authors. All rights reserved.
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
package areca.demo;

import java.util.TreeMap;

import org.teavm.jso.dom.html.HTMLElement;

import areca.common.Timer;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.rt.teavm.ui.UIComponentRenderer.ScrollableHTMLElement;
import areca.ui.component2.ScrollableComposite;

/**
 *
 * @author Falko Bräutigam
 */
public class SyncScrolling {

    private static final Log LOG = LogFactory.getLog( SyncScrolling.class );

    /**
     *
     */
    class Part {

        ScrollableComposite             scrollable;

        /** Vertical positions of the child elements */
        TreeMap<Integer,HTMLElement>    elements = new TreeMap<>();

        public Part( ScrollableComposite scrollable ) {
            this.scrollable = scrollable;

            var headers = ((HTMLElement)scrollable.htmlElm).getElementsByTagName( "h2" );
            for (int i = 0; i < headers.getLength(); i++) {
                HTMLElement h = headers.item( i );
                elements.put( h.getOffsetTop(), h );
                h.setAttribute( "sync-scrolling-index", String.valueOf( i ) );
            }
        }
    }

    // instance *******************************************

    protected Part          left, right;

    protected volatile int  skipEvent;


    public SyncScrolling( ScrollableComposite one, ScrollableComposite two ) {
        var start = Timer.start();
        left = new Part( one );
        right = new Part( two );

        left.scrollable.scrollTop.onChange( (current,__) -> sync( left, right, current ) );
        right.scrollable.scrollTop.onChange( (current,__) -> sync( right, left, current ) );
        LOG.info( "init: done (%s)", start.elapsedHumanReadable() );
    }


    protected void sync( Part origin, Part target, Integer top ) {
        if (skipEvent-- >= 0) {
            return;
        }
        LOG.info( "Scroll: top = %s", top );
        var floor = origin.elements.floorEntry( top + (origin.scrollable.size.$().height() / 2) );
        if (floor.getKey() > top) {
            var index = floor.getValue().getAttribute( "sync-scrolling-index" );
            for (var elm : target.elements.values()) {
                if (elm.getAttribute( "sync-scrolling-index" ).equals( index )) {
                    LOG.info( "      : index = %s", index );
                    skipEvent = 1;
                    ((ScrollableHTMLElement)elm).scrollIntoView( "smooth", "start" );
                }
            }
        }
    }

}
