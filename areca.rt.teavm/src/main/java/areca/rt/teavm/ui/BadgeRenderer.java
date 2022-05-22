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

import org.teavm.jso.dom.html.HTMLElement;

import areca.common.event.EventHandler;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.ui.component2.Badge;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComponentEvent;
import areca.ui.component2.UIComponentEvent.ComponentConstructedEvent;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class BadgeRenderer {

    private static final Log LOG = LogFactory.getLog( BadgeRenderer.class );

    public static final ClassInfo<BadgeRenderer> TYPE = BadgeRendererClassInfo.instance();

    static void _start() {
        UIComponentEvent.manager
                .subscribe( new BadgeRenderer() )
                .performIf( ev -> {
                    if (ev instanceof ComponentConstructedEvent) {
                        return ((UIComponentEvent)ev).getSource().decorators().anyMatches( Badge.class::isInstance );
                    }
                    return false;
                });
    }


    // instance *******************************************

    @EventHandler( ComponentConstructedEvent.class )
    public void componentConstructed( ComponentConstructedEvent ev ) {
        UIComponent c = ev.getSource();
        Badge badge = (Badge)c.decorators().filter( Badge.class::isInstance ).single();

        badge.content.onInitAndChange( (content,__) -> {
            if (content != null) {
                ((HTMLElement)c.htmlElm).setAttribute( "data-badge", content );
                c.cssClasses.add( "Badged" );
                c.cssClasses.add( "Badged-NorthEast" );
            } else {
                ((HTMLElement)c.htmlElm).removeAttribute( "data-badge" );
                c.cssClasses.remove( "Badged" );
                c.cssClasses.remove( "Badged-NorthEast" );
            }
        });
    }
}
