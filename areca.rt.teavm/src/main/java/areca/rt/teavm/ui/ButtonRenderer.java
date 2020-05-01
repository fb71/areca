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

import org.teavm.jso.dom.events.MouseEvent;
import org.teavm.jso.dom.html.HTMLButtonElement;
import org.teavm.jso.dom.html.HTMLElement;

import areca.common.event.EventManager;
import areca.ui.Button;
import areca.ui.Property.PropertyChangedEvent;
import areca.ui.SelectionEvent;
import areca.ui.UIRenderEvent.ComponentCreatedEvent;

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
    protected void handleComponentCreated( ComponentCreatedEvent ev, Button button ) {
        // XXX check that none exists yet
        button.data( DATA_ELM, () -> {
            HTMLElement parentElement = htmlElementOf( button.parent() );
            HTMLButtonElement elm = (HTMLButtonElement) parentElement.appendChild( doc().createElement( "button" ) );
            elm.addEventListener( "click", (MouseEvent mev) -> {
                EventManager.instance().publish( new SelectionEvent( button ) );
            });
            return elm;
        });

        super.handleComponentCreated( ev, button );
    }


    @Override
    protected void handlePropertyChanged( PropertyChangedEvent ev, Button button ) {
        super.handlePropertyChanged( ev, button );

        HTMLButtonElement elm = htmlElementOf( button );

        if (Button.TYPE.label.equals( ev.getSource() )) {
            elm.appendChild(doc().createTextNode( ev.getNewValue() ) );
        }
    }

}
