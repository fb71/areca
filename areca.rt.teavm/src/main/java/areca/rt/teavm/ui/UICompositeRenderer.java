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
import org.teavm.jso.dom.xml.Node;

import areca.common.Assert;
import areca.common.event.EventHandler;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.ui.component2.UIComponentEvent;
import areca.ui.component2.UIComponentEvent.ComponentConstructedEvent;
import areca.ui.component2.UIComposite;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class UICompositeRenderer
        extends RendererBase {

    private static final Log LOG = LogFactory.getLog( UICompositeRenderer.class );

    public static final ClassInfo<UICompositeRenderer> TYPE = UICompositeRendererClassInfo.instance();

    static void _start() {
        UIComponentEvent.manager()
                .subscribe( new UICompositeRenderer( ) )
                .performIf( ev -> ev instanceof ComponentConstructedEvent && ev.getSource() instanceof UIComposite );
    }

    // instance *******************************************

    @EventHandler( ComponentConstructedEvent.class )
    public void componentConstructed( ComponentConstructedEvent ev ) {
        UIComposite c = (UIComposite)ev.getSource();
        c.htmlElm = (HTMLElement)doc().createElement( "div" );

        // XXX
        if (ev.getSource().getClass().getSimpleName().startsWith( "Root" )) {
            LOG.warn( "ROOT container:  " + ev.getSource().getClass().getSimpleName() );
            Assert.that( doc().getElementById( "body" ) == null, "RootWindow other than document.body is not supported yet (because initial size)" );
            doc().getBody().appendChild( (Node)c.htmlElm );

//            LOG.warn( "" + Size.of( doc().getBody().getClientWidth(), doc().getBody().getClientHeight() ) );
//            c.size.defaultsTo( () -> {
//                return Size.of( doc().getBody().getClientWidth(), doc().getBody().getClientHeight() );
//            });
        }
    }

}
