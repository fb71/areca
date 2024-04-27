/*
 * Copyright (C) 2024, the @authors. All rights reserved.
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

import org.teavm.jso.dom.html.HTMLIFrameElement;

import areca.common.Assert;
import areca.common.event.EventHandler;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.ui.component2.IFrame;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComponentEvent;
import areca.ui.component2.UIComponentEvent.ComponentConstructedEvent;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class IFrameRenderer
        extends RendererBase {

    private static final Log LOG = LogFactory.getLog( IFrameRenderer.class );

    public static final ClassInfo<IFrameRenderer> TYPE = IFrameRendererClassInfo.instance();

    public static void _start() {
        UIComponentEvent.manager()
                .subscribe( new IFrameRenderer() )
                .performIf( ComponentConstructedEvent.class, ev -> ev.getSource() instanceof IFrame );
    }


    // instance *******************************************

    @SuppressWarnings( "unchecked" )
    protected HTMLIFrameElement htmlElm( UIComponent c ) {
        return Assert.notNull( (HTMLIFrameElement)c.htmlElm );
    }


    @EventHandler( ComponentConstructedEvent.class )
    public void componentConstructed( ComponentConstructedEvent ev ) {
        var c = (IFrame)ev.getSource();

        var iframe = (HTMLIFrameElement)doc().createElement( "iframe" );
        c.htmlElm = iframe;

        c.src.onInitAndChange( (newValue, oldValue) -> {
            iframe.setAttribute( "src", newValue );
        });

        c.reloadCount.onChange( (newValue, oldValue) -> {
            LOG.info( "reloadCount: %s", newValue );
            iframe.getContentWindow().getLocation().reload();
        });
    }

}
