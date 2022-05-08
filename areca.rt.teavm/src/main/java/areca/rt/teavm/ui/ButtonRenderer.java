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

import org.teavm.jso.dom.html.HTMLButtonElement;
import org.teavm.jso.dom.html.HTMLElement;

import areca.common.event.EventHandler;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.ui.component2.Button;
import areca.ui.component2.UIComponentEvent;
import areca.ui.component2.UIComponentEvent.ComponentConstructedEvent;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class ButtonRenderer
        extends UIComponentRenderer {

    private static final Log LOG = LogFactory.getLog( ButtonRenderer.class );

    public static final ClassInfo<ButtonRenderer> TYPE = ButtonRendererClassInfo.instance();

    static void _start() {
        UIComponentEvent.manager
                .subscribe( new ButtonRenderer() )
                .performIf( ev -> ev instanceof UIComponentEvent && ev.getSource() instanceof Button );
    }

    // instance *******************************************

    @Override
    @EventHandler( ComponentConstructedEvent.class )
    public void componentConstructed( ComponentConstructedEvent ev ) {
        Button c = (Button)ev.getSource();

        var htmlButton = (HTMLButtonElement)doc().createElement( "button" );
        c.htmlElm = htmlButton;

        var textNode = (HTMLElement)doc().createTextNode( c.label.opt().orElse( "" ) );
        htmlButton.appendChild( textNode );

        c.label.onChange( (newValue, oldValue) -> {
            textNode.setNodeValue( newValue );
        });

        super.componentConstructed( ev );
    }
}
