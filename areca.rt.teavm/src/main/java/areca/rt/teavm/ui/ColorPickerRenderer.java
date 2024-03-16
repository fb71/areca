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

import org.teavm.jso.dom.html.HTMLInputElement;

import areca.common.event.EventHandler;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.ui.component2.ColorPicker;
import areca.ui.component2.Events.EventType;
import areca.ui.component2.UIComponentEvent;
import areca.ui.component2.UIComponentEvent.ComponentConstructedEvent;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class ColorPickerRenderer
        extends RendererBase {

    private static final Log LOG = LogFactory.getLog( ColorPickerRenderer.class );

    public static final ClassInfo<ColorPickerRenderer> TYPE = ColorPickerRendererClassInfo.instance();

    public static void _start() {
        UIComponentEvent.manager()
                .subscribe( new ColorPickerRenderer() )
                .performIf( ComponentConstructedEvent.class, ev -> ev.getSource() instanceof ColorPicker );
    }

    // instance *******************************************

    @EventHandler( ComponentConstructedEvent.class )
    public void componentConstructed( ComponentConstructedEvent ev ) {
        var c = (ColorPicker)ev.getSource();

        var input = (HTMLInputElement)(c.htmlElm = doc().createElement( "input" ));
        input.setAttribute( "type", "color" );

        c.value.onInitAndChange( (newValue, oldValue) -> {
            LOG.debug( "Set: %s", newValue );
            input.setValue( newValue != null ? newValue : "" );
        });

        LOG.debug( "Register listener: %s", "input" );
        input.addEventListener( "change", htmlEv -> {
            htmlEv.stopPropagation();
            htmlEv.preventDefault();

            c.value.rawSet( input.getValue() );
            LOG.debug( "Text: %s", input.getValue() );
            propagateEvent( c, htmlEv, EventType.TEXT );
        });
    }

}
