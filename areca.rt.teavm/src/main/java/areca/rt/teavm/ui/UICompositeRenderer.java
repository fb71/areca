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

import areca.ui.UIComposite;
import areca.ui.Property.PropertySetEvent;
import areca.ui.UIRenderEvent.ComponentCreated;

/**
 *
 * @author falko
 */
public class UICompositeRenderer
        extends UIComponentRenderer<UIComposite> {

    private static final Logger LOG = Logger.getLogger( UICompositeRenderer.class.getSimpleName() );


    protected UICompositeRenderer() {
        super( UIComposite.class );
    }


    @Override
    protected void handleComponentCreated( ComponentCreated ev, UIComposite composite ) {
        // XXX check that none exists yet
        HTMLElement div = composite.getOrCreateData( DATA_ELM, () -> {
            HTMLElement newDiv = doc().createElement( "div" );
            // root
            if (composite.parent() == null) {
                return (HTMLElement)doc().getBody().appendChild( newDiv );
            }
            // other
            else {
                HTMLElement parentElement = htmlElementOf( composite.parent() );
                return (HTMLElement)parentElement.appendChild( newDiv );
            }
        });
        assert div != null;
        assert composite.getOrCreateData( DATA_ELM ) != null;

        super.handleComponentCreated( ev, composite );

        if (composite.parent() == null) {
            div.getStyle().setProperty( "position", "relative" );
        }
    }


    @Override
    protected void handlePropertyChange( PropertySetEvent ev, UIComposite composite ) {
        super.handlePropertyChange( ev, composite );

        HTMLElement elm = htmlElementOf( composite );
        // ...
    }

}
