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

import org.teavm.jso.dom.html.HTMLAnchorElement;
import org.teavm.jso.dom.html.HTMLElement;

import areca.common.Assert;
import areca.common.event.EventHandler;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.ui.component2.Link;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComponentEvent;
import areca.ui.component2.UIComponentEvent.ComponentConstructedEvent;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class LinkRenderer
        extends RendererBase {

    private static final Log LOG = LogFactory.getLog( LinkRenderer.class );

    public static final ClassInfo<LinkRenderer> TYPE = LinkRendererClassInfo.instance();

    static void _start() {
        UIComponentEvent.manager()
                .subscribe( new LinkRenderer() )
                .performIf( ev -> ev instanceof ComponentConstructedEvent && ev.getSource() instanceof Link );
    }


    // instance *******************************************

    protected HTMLAnchorElement htmlElm( UIComponent c ) {
        return Assert.notNull( (HTMLAnchorElement)c.htmlElm );
    }

    @EventHandler( ComponentConstructedEvent.class )
    public void componentConstructed( ComponentConstructedEvent ev ) {
        Link c = (Link)ev.getSource();

        c.htmlElm = (HTMLAnchorElement)doc().createElement( "a" );
        var textNode = (HTMLElement)doc().createTextNode( c.content.opt().orElse( "" ) );
        htmlElm( c ).appendChild( textNode );
        htmlElm( c ).setAttribute( "href", c.href.opt().orElse( "#" ) );

        c.content.onChange( (newValue, oldValue) -> {
            textNode.setNodeValue( newValue );
        });
    }

}
