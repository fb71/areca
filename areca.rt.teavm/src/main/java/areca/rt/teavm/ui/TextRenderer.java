/*
 * Copyright (C) 2020, the @authors. All rights reserved.
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

import areca.ui.Size;
import areca.ui.Property.PropertyChangedEvent;
import areca.ui.component.Text;
import areca.ui.component.UIRenderEvent.ComponentCreatedEvent;

/**
 *
 * @author falko
 */
public class TextRenderer
        extends UIComponentRenderer<Text,HTMLElement> {

    protected TextRenderer() {
        super( Text.class );
    }


    @Override
    protected HTMLElement handleComponentCreated( ComponentCreatedEvent ev, Text text, HTMLElement div ) {
        HTMLElement parentElm = htmlElementOf( text.parent() );
        div = (HTMLElement)parentElm.appendChild( doc().createElement( "div" ) );
        return super.handleComponentCreated( ev, text, div );
    }


    @Override
    protected void handlePropertyChanged( PropertyChangedEvent ev, Text text, HTMLElement div ) {
        super.handlePropertyChanged( ev, text, div );

        // text
        if (Text.TYPE.text.equals( ev.getSource() )) {
            log( text, "SET " + ev.getNewValue() );
            div.appendChild( doc().createTextNode( ev.getNewValue() ) );

            // XXX this is ok only if size was not yet set by layout
            Size size = Size.of( div.getOffsetWidth(), div.getOffsetHeight() );
            text.size.rawSet( size );
            text.minSize.rawSet( size );
            log( text, "UPDATE " + text.size.get() );
        }
    }

}
