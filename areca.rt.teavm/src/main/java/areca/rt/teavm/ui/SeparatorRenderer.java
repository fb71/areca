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

import static areca.ui.Orientation.HORIZONTAL;

import org.teavm.jso.dom.html.HTMLElement;

import areca.common.Assert;
import areca.common.event.EventHandler;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.ui.component2.Separator;
import areca.ui.component2.UIComponentEvent;
import areca.ui.component2.UIComponentEvent.ComponentConstructedEvent;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class SeparatorRenderer
        extends RendererBase {

    private static final Log LOG = LogFactory.getLog( SeparatorRenderer.class );

    public static final ClassInfo<SeparatorRenderer> TYPE = SeparatorRendererClassInfo.instance();

    public static void _start() {
        UIComponentEvent.manager()
                .subscribe( new SeparatorRenderer() )
                .performIf( ev -> ev instanceof ComponentConstructedEvent && ev.getSource() instanceof Separator );
    }

    // instance *******************************************

    @EventHandler( ComponentConstructedEvent.class )
    public void componentConstructed( ComponentConstructedEvent ev ) {
        Separator c = (Separator)ev.getSource();

        Assert.that( c.orientation.value() == HORIZONTAL, "Separator: VERTICAL orientation is not yet supported." );
        c.htmlElm = (HTMLElement)doc().createElement( "HR" );
    }

}
