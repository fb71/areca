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
package areca.ui.component;

import java.util.logging.Logger;

import areca.ui.component.Property.ReadWrite;
import areca.ui.html.HtmlElement;
import areca.ui.html.HtmlElement.Type;
import areca.ui.html.HtmlNode;
import areca.ui.html.HtmlTextNode;

/**
 *
 * @author falko
 */
public class Text
        extends UIComponent {

    private static final Logger LOG = Logger.getLogger( Text.class.getSimpleName() );

    @SuppressWarnings("hiding")
    public static final Text TYPE = new Text();

    protected HtmlTextNode      textNode;

    public ReadWrite<String> text = Property.create( this, "text",
            () -> textNode.value.get(),
            v -> textNode.value.set( v ) );


    @Override
    protected HtmlNode init( UIComposite newParent ) {
        htmlElm = new HtmlElement( Type.DIV );
        htmlElm.children.add( textNode = new HtmlTextNode( "" ) );
        super.init( newParent );
        return htmlElm;
    }

}
