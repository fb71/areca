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

import org.teavm.jso.dom.html.HTMLBodyElement;
import org.teavm.jso.dom.html.HTMLElement;

import areca.ui.Size;
import areca.ui.component.UIComposite;
import areca.ui.component.Property.PropertyChangedEvent;
import areca.ui.component.UIRenderEvent.ComponentCreatedEvent;
import areca.common.Assert;

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
    protected void handleComponentCreated( ComponentCreatedEvent ev, UIComposite composite ) {
        // XXX check that none exists yet
        HTMLElement div = composite.data( DATA_ELM, () -> {
            HTMLElement newDiv = doc().createElement( "div" );
            // root window
            if (composite.parent() == null) {
                HTMLBodyElement body = doc().getBody();

                Size bodySize = Size.of( body.getClientWidth(), body.getClientHeight() );
                newDiv.getStyle().setProperty( "height", bodySize.height() + "px" );
                composite.size.rawSet( bodySize );
                LOG.info( "Root window: " + bodySize );

                return (HTMLElement)body.appendChild( newDiv );
            }
            // other
            else {
                HTMLElement parentElement = htmlElementOf( composite.parent() );
                return (HTMLElement)parentElement.appendChild( newDiv );
            }
        });
        Assert.notNull( div );
        Assert.that( composite.optData( DATA_ELM ).isPresent() );

        super.handleComponentCreated( ev, composite );

        // root window
        if (composite.parent() == null) {
            div.getStyle().setProperty( "position", "relative" );
            LOG.info( "rdiv: " + div.getClientWidth() + " / " + div.getClientHeight() );
        }
    }


    @Override
    protected void handlePropertyChanged( PropertyChangedEvent ev, UIComposite composite ) {
        super.handlePropertyChanged( ev, composite );

        HTMLElement elm = htmlElementOf( composite );
        // ...
    }

}
