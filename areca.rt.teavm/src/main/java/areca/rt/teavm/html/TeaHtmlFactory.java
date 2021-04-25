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

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

import areca.ui.html.HtmlButton;
import areca.ui.html.HtmlElement;
import areca.ui.html.HtmlFactory;
import areca.ui.html.HtmlNode;
import areca.ui.html.HtmlTextNode;

/**
 *
 * @author Falko Bräutigam
 */
public class TeaHtmlFactory
        implements HtmlFactory {

    public static HTMLDocument      doc = Window.current().getDocument();


    @Override
    public void init( HtmlNode elm ) {
        if (elm instanceof HtmlButton) {
            HtmlButtonImpl.init( (HtmlButton)elm, doc );
        }
        else if (elm instanceof HtmlTextNode) {
            HtmlTextNodeImpl.init( (HtmlTextNode)elm, doc );
        }
        else if (elm instanceof HtmlElement) {
            HtmlElementImpl.init( (HtmlElement)elm, doc );
        }
    }


    @Override
    public void dispose( HtmlNode elm ) {
//        Assert.notNull( elm.parent, "Parent is null. Already disposed!?" )
//                .<HTMLElement>delegate().removeChild( elm.<HTMLElement>delegate() );
//        elm.parent = null;

        elm.<HTMLElement>delegate().delete();
        elm.delegate = null;
    }

}
