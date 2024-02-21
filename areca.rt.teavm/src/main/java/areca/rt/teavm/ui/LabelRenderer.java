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

import areca.common.Assert;
import areca.common.event.EventHandler;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.ui.component2.Label;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComponentEvent;
import areca.ui.component2.UIComponentEvent.DecoratorAttachedEvent;
import areca.ui.component2.UIComponentEvent.DecoratorDetachedEvent;
import areca.ui.component2.UIComponentEvent.DecoratorEventBase;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class LabelRenderer {

    private static final Log LOG = LogFactory.getLog( LabelRenderer.class );

    public static final ClassInfo<LabelRenderer> TYPE = LabelRendererClassInfo.instance();

    static void _start() {
        UIComponentEvent.manager()
                .subscribe( new LabelRenderer() )
                .performIf( DecoratorEventBase.class, ev -> ev.getSource() instanceof Label );
    }


    // instance *******************************************

    @EventHandler( DecoratorAttachedEvent.class )
    public void attached( DecoratorAttachedEvent ev ) {
        Label label = (Label)ev.getSource();
        UIComponent c = Assert.isEqual( ev.decorated, label.decorated() );

        label.content.onInitAndChange( (content,__) -> {
            if (content != null) {
                ((HTMLElement)c.htmlElm).setAttribute( "data-label", content );
                c.cssClasses.add( "Labeled" );
            } else {
                ((HTMLElement)c.htmlElm).removeAttribute( "data-label" );
                c.cssClasses.remove( "Labeled" );
            }
        });
    }


    @EventHandler( DecoratorDetachedEvent.class )
    public void detached( DecoratorDetachedEvent ev ) {
        UIComponent c = ev.decorated;

        ((HTMLElement)c.htmlElm).removeAttribute( "data-label" );
        c.cssClasses.remove( "Labeled" );
    }
}
