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

import org.teavm.jso.dom.xml.Document;
import org.teavm.jso.dom.xml.Text;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Property;
import areca.ui.html.HtmlTextNode;

/**
 *
 * @author Falko Bräutigam
 */
public class HtmlTextNodeImpl {

    private static final Log log = LogFactory.getLog( HtmlTextNodeImpl.class );

    public static void init( HtmlTextNode elm, Document doc ) {
        log.debug( "elm= " + elm );

        Text delegate = doc.createTextNode("");
        elm.delegate = delegate;

        //HtmlElementImpl.init( elm, delegate );

        elm.value = Property.create( elm, "value", () -> delegate.getNodeValue(), v -> delegate.setNodeValue( v ) );
    }

}
