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
import areca.ui.html.HtmlButton;
import areca.ui.html.HtmlNode;
import areca.ui.html.HtmlTextNode;

/**
 *
 * @author falko
 *
 */
public class Button
        extends UIComponent {

    private static final Logger LOG = Logger.getLogger( Button.class.getSimpleName() );

//    @SuppressWarnings("hiding")
//    public static final Button  TYPE = new Button();

    public ReadWrite<String> label = new ReadWrite<>( this, "label" ) {
        private String value;
        @Override
        protected void doSet( String newValue ) {
            value = newValue;
            HtmlTextNode text = new HtmlTextNode( newValue );
            htmlElm.children.add( text );
        }
        @Override
        protected String doGet() {
            return value;
        }
    };


    @Override
    protected HtmlNode init( UIComposite newParent ) {
        htmlElm = new HtmlButton();
        bordered.rawSet( true );
        super.init( newParent );
        return htmlElm;
    }

}
