/*
 * polymap.org
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
package areca.ui.layout;

import java.util.logging.Logger;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLElement;

import areca.ui.Orientation;
import areca.ui.UIComponent;
import areca.ui.UIComposite;
import areca.ui.client.teavm.UIComponentRenderer;

/**
 *
 * @author falko
 */
public class FillLayout
        extends LayoutManager {

    private static final Logger LOG = Logger.getLogger( FillLayout.class.getSimpleName() );

    private Orientation             orientation = Orientation.HORIZONTAL;


    @Override
    public void layout( UIComposite composite ) {
        LOG.info( "Body: " + Window.current().getDocument().getBody().getClientWidth() );

        LOG.info( "Composite: clientHeight= " + UIComponentRenderer.htmlElementOf( composite ).getClientWidth() );

        for (UIComponent component : composite.components()) {
            HTMLElement elm = UIComponentRenderer.htmlElementOf( component );
            LOG.info( "Component: " + elm.getClientWidth() + " / " + elm.getClientHeight() );
        }

//        Size parentSize = composite.parent().size.get();
//
//        if (orientation == Orientation.HORIZONTAL) {
//            int componentWidth = parentSize.width() / composite.components().size();
//            for (UIComponent component : composite.components()) {
//                component.size.set( new Size( componentWidth, parentSize.height() ) );
//            }
//        }
//        else {
//            throw new RuntimeException( "not yet implemented." );
//        }
    }

}
