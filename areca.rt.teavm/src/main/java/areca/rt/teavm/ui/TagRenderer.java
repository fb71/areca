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
import areca.ui.component2.UIComponentEvent;
import areca.ui.component2.UIComponentEvent.DecoratorAttachedEvent;
import areca.ui.component2.UIComponentEvent.DecoratorEventBase;

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
                .performIf( DecoratorEventBase.class, ev -> ev.getSource() instanceof Tag );
    }


    // instance *******************************************

    @EventHandler( DecoratorAttachedEvent.class )
    public void attached( DecoratorAttachedEvent ev ) {
        var tag = (Tag)ev.getSource();
        var c = tag.decorated();

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


//    @EventHandler( DecoratorDetachedEvent.class )
//    public void detached( DecoratorDetachedEvent ev ) {
//        var badge = (Badge)ev.getSource();
//        var c = badge.decorated();
//    }
}
