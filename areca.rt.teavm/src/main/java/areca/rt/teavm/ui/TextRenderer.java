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

import java.util.logging.Logger;

import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.xml.Node;

import areca.ui.Text;
import areca.ui.UIRenderEvent.ComponentCreatedEvent;

/**
 *
 * @author falko
 */
public class TextRenderer
        extends UIComponentRenderer<Text> {

    private static final Logger LOG = Logger.getLogger( TextRenderer.class.getSimpleName() );

    protected TextRenderer() {
        super( Text.class );
    }

    @Override
    protected void handleComponentCreated( ComponentCreatedEvent ev, Text text ) {
        // XXX check that none exists yet
        text.data( DATA_ELM, () -> {
            HTMLElement parentElement = htmlElementOf( text.parent() );
            Node result = parentElement.appendChild( doc().createElement( "div" ) );
            return result;
        });

        super.handleComponentCreated( ev, text );
    }

}
