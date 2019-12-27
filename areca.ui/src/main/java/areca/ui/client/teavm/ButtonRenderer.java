/*
 * polymap.org
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
package areca.ui.client.teavm;

import java.util.logging.Logger;

import org.teavm.jso.dom.html.HTMLButtonElement;
import org.teavm.jso.dom.html.HTMLElement;

import areca.ui.Button;
import areca.ui.Property.PropertySetEvent;
import areca.ui.UIRenderEvent.ComponentCreated;

/**
 *
 * @author falko
 */
public class ButtonRenderer
        extends UIComponentRenderer<Button> {

    private static final Logger LOG = Logger.getLogger( ButtonRenderer.class.getSimpleName() );


    protected ButtonRenderer() {
        super( Button.class );
    }


    @Override
    protected void handleComponentCreated( ComponentCreated ev, Button button ) {
        // XXX check that none exists yet
        button.data( DATA_ELM, () -> {
            HTMLElement parentElement = htmlElementOf( button.parent() );
            return (HTMLButtonElement) parentElement.appendChild( doc().createElement( "button" ) );
        });

        super.handleComponentCreated( ev, button );
    }


    @Override
    protected void handlePropertyChange( PropertySetEvent ev, Button button ) {
        super.handlePropertyChange( ev, button );

        HTMLButtonElement elm = htmlElementOf( button );

        if (Button.TYPE.label.equals( ev.getSource() )) {
            elm.appendChild(doc().createTextNode( ev.getNewValue() ) );
        }
    }

}
