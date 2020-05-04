/*
 * Copyright (C) 2019, the @authors. All rights reserved.
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

import java.util.logging.Logger;

import org.teavm.jso.dom.html.HTMLElement;

import areca.ui.Size;
import areca.ui.component.UIComposite;
import areca.ui.component.UIRenderEvent.ComponentCreatedEvent;

/**
 *
 * @author falko
 */
public class UICompositeRenderer
        extends UIComponentRenderer<UIComposite,HTMLElement> {

    private static final Logger LOG = Logger.getLogger( UICompositeRenderer.class.getSimpleName() );


    protected UICompositeRenderer() {
        super( UIComposite.class );
    }


    @Override
    protected HTMLElement handleComponentCreated( ComponentCreatedEvent ev, UIComposite composite, HTMLElement div ) {
        HTMLElement parentElm = composite.parent() == null
                ? doc().getBody()
                : htmlElementOf( composite.parent() );

        div = doc().createElement( "div" );
        parentElm.appendChild( div );
        super.handleComponentCreated( ev, composite, div );

        // root window
        if (composite.parent() == null) {
            Size bodySize = Size.of( parentElm.getClientWidth(), parentElm.getClientHeight() );
            div.getStyle().setProperty( "height", bodySize.height() + "px" );
            composite.size.rawSet( bodySize );
            LOG.info( "Root window: " + bodySize );
            div.getStyle().setProperty( "position", "relative" );
            LOG.info( "rdiv: " + div.getClientWidth() + " / " + div.getClientHeight() );

            div.getStyle().setProperty( "overflow", "scroll" );
        }
        return div;
    }

}
