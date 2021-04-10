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
import areca.ui.Size;
import areca.ui.component.Button;
import areca.ui.component.Property.PropertyChangedEvent;
import areca.ui.component.SelectionEvent;
import areca.ui.component.UIRenderEvent.ComponentCreatedEvent;

/**
 *
 * @author falko
 */
public class ButtonRenderer
        extends UIComponentRenderer<Button, HTMLButtonElement> {

    private static final Logger LOG = Logger.getLogger( ButtonRenderer.class.getSimpleName() );


    protected ButtonRenderer() {
        super( Button.class );
    }


    @Override
    protected HTMLButtonElement handleComponentCreated( ComponentCreatedEvent ev, Button button, HTMLButtonElement elm ) {
        HTMLElement parentElm = htmlElementOf( button.parent() );
        elm = (HTMLButtonElement)parentElm.appendChild( doc().createElement( "button" ) );
        elm.addEventListener( "click", (MouseEvent mev) -> {
            EventManager.instance().publish( new SelectionEvent( button ) );
        });
        return super.handleComponentCreated( ev, button, elm );
    }


    @Override
    protected void handlePropertyChanged( PropertyChangedEvent ev, Button button, HTMLButtonElement elm ) {
        super.handlePropertyChanged( ev, button, elm );

        if (Button.TYPE.label.equals( ev.getSource() )) {
            log( button, "SET " + ev.getNewValue() );
            elm.appendChild( doc().createTextNode( ev.getNewValue() ) );

            // XXX this is ok only if size was not yet set by layout
            Size size = Size.of( elm.getOffsetWidth(), elm.getOffsetHeight() );
            button.size.rawSet( size );
            button.minSize.rawSet( size );
            log( button, "UPDATE " + button.size.get() );
        }
    }

}
