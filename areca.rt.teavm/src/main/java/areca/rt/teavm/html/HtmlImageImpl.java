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

import org.teavm.jso.dom.html.HTMLImageElement;
import org.teavm.jso.dom.xml.Document;

import areca.ui.Property;
import areca.ui.html.HtmlImage;

/**
 *
 * @author Falko Bräutigam
 */
public class HtmlImageImpl {

    public static void init( HtmlImage elm, Document doc ) {
        var delegate = (HTMLImageElement)doc.createElement( "img" );
        HtmlElementImpl.init( elm, delegate );

        elm.src = Property.create( elm, "src", () -> delegate.getSrc(), v -> delegate.setSrc( v ) );
    }

}
