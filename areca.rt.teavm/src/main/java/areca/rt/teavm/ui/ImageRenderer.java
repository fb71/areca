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

import org.teavm.jso.dom.html.HTMLImageElement;

import areca.common.event.EventHandler;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.ui.component2.Image;
import areca.ui.component2.UIComponentEvent;
import areca.ui.component2.UIComponentEvent.ComponentConstructedEvent;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class ImageRenderer
        extends RendererBase {

    private static final Log LOG = LogFactory.getLog( ImageRenderer.class );

    public static final ClassInfo<ImageRenderer> TYPE = ImageRendererClassInfo.instance();

    public static void _start() {
        UIComponentEvent.manager()
                .subscribe( new ImageRenderer() )
                .performIf( ComponentConstructedEvent.class, ev -> ev.getSource() instanceof Image );
    }

    // instance *******************************************

    @EventHandler( ComponentConstructedEvent.class )
    public void componentConstructed( ComponentConstructedEvent ev ) {
        Image c = (Image)ev.getSource();

        var img = (HTMLImageElement)doc().createElement( "img" );
        c.htmlElm = img;

        // data
        c.data.onInitAndChange( (newValue, __) -> {
            if (newValue != null) {
                img.setSrc( "data:;base64," + newValue );
                //img.getStyle().setProperty( "height", "100%" );
            }
            else {
                img.delete();
            }
        });
        // src
        c.src.onInitAndChange( (newValue, __) -> {
            if (newValue != null) {
                img.setSrc( newValue );
            }
            else {
                img.delete();
            }
        });
    }
}
