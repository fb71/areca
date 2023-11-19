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

import areca.common.base.Sequence;
import areca.common.event.EventHandler;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.ui.component2.Tag;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComponentEvent;
import areca.ui.component2.UIComponentEvent.ComponentConstructedEvent;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class TagRenderer {

    private static final Log LOG = LogFactory.getLog( TagRenderer.class );

    public static final ClassInfo<TagRenderer> TYPE = TagRendererClassInfo.instance();

    static void _start() {
        UIComponentEvent.manager()
                .subscribe( new TagRenderer() )
                .performIf( ev -> {
                    if (ev instanceof ComponentConstructedEvent) {
                        return ((UIComponentEvent)ev).getSource().decorators().anyMatches( Tag.class::isInstance );
                    }
                    return false;
                });
    }


    // instance *******************************************

    @EventHandler( ComponentConstructedEvent.class )
    public void componentConstructed( ComponentConstructedEvent ev ) {
        UIComponent c = ev.getSource();
        Tag tag = (Tag)c.decorators().filter( Tag.class::isInstance ).single();

        tag.icons.onInitAndChange( (icons,__) -> {
            if (icons.size() > 1) {
                throw new RuntimeException( "More than 1 icon is not yet supported." );
            }
            ((HTMLElement)c.htmlElm).removeAttribute( "data-tag" );
            c.cssClasses.remove( "Tagged" );
            c.cssClasses.remove( "Tagged-BottomRight" );

            Sequence.of( icons ).first().ifPresent( icon -> {
                ((HTMLElement)c.htmlElm).setAttribute( "data-tag", icon );
                c.cssClasses.add( "Tagged" );
                c.cssClasses.add( "Tagged-BottomRight" );
            });
        });
    }
}
