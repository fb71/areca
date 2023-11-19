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
package areca.rt.teavm.ui;

import java.util.EventObject;

import areca.common.event.EventCollector;
import areca.common.event.EventHandler;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.ui.component2.ScrollableComposite;
import areca.ui.component2.UIComponentEvent;
import areca.ui.component2.UIComponentEvent.ComponentConstructedEvent;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class ScrollableCompositeRenderer
        extends RendererBase {

    private static final Log LOG = LogFactory.getLog( ScrollableCompositeRenderer.class );

    public static final ClassInfo<ScrollableCompositeRenderer> TYPE = ScrollableCompositeRendererClassInfo.instance();

    static void _start() {
        UIComponentEvent.manager()
                .subscribe( new ScrollableCompositeRenderer() )
                .performIf( ev -> ev instanceof ComponentConstructedEvent && ev.getSource() instanceof ScrollableComposite );
    }

    // instance *******************************************

    @EventHandler( ComponentConstructedEvent.class )
    public void componentConstructed( ComponentConstructedEvent ev ) {
        ScrollableComposite c = (ScrollableComposite)ev.getSource();
        var div = htmlElm( c );

        var throttle = new EventCollector<>( 300 );
        div.addEventListener( "scroll", htmlEv -> {
            throttle.collect( new EventObject( c ), l -> {
                LOG.debug( "Throttle: " + l.size() );
                c.scrollLeft.set( div.getScrollLeft() );
                c.scrollTop.set( div.getScrollTop() );
            });
        });
    }

}
