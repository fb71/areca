/*
 * Copyright (C) 2021, the @authors. All rights reserved.
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
package areca.rt.teavm.html;

import org.teavm.jso.dom.html.HTMLButtonElement;
import org.teavm.jso.dom.xml.Document;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Property;
import areca.ui.html.HtmlButton;

/**
 *
 * @author Falko Bräutigam
 */
public class HtmlButtonImpl {

    private static final Log log = LogFactory.getLog( HtmlButtonImpl.class );


    public static void init( HtmlButton elm, Document doc ) {
        log.debug( "elm= " + elm );
        HTMLButtonElement delegate = (HTMLButtonElement)doc.createElement( "button" );
        HtmlElementImpl.init( elm, delegate );

        elm.value = Property.create( elm, "value", () -> delegate.getValue(), v -> delegate.setValue( v ) );
    }

}
