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
import areca.ui.component2.Progress;
import areca.ui.component2.UIComponentEvent;
import areca.ui.component2.UIComponentEvent.ComponentConstructedEvent;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class ProgressRenderer
        extends RendererBase {

    private static final Log LOG = LogFactory.getLog( ProgressRenderer.class );

    public static final ClassInfo<ProgressRenderer> TYPE = ProgressRendererClassInfo.instance();

    static void _start() {
        UIComponentEvent.manager()
                .subscribe( new ProgressRenderer() )
                .performIf( ev -> ev instanceof ComponentConstructedEvent && ev.getSource() instanceof Progress );
    }

    // instance *******************************************

    @EventHandler( ComponentConstructedEvent.class )
    public void componentConstructed( ComponentConstructedEvent ev ) {
        Progress c = (Progress)ev.getSource();

        c.htmlElm = (HTMLElement)doc().createElement( "progress" );
        var textNode = (HTMLElement)doc().createTextNode( "" );
        htmlElm( c ).appendChild( textNode );

        c.max.onInitAndChange( (newValue, __) -> {
            htmlElm( c ).setAttribute( "max", newValue.toString() );
        });
        c.value.onInitAndChange( (newValue, __) -> {
            htmlElm( c ).setAttribute( "value", newValue.toString() );
            //textNode.setNodeValue( newValue );
        });
    }

}
