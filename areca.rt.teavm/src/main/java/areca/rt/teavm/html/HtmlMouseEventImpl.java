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

import org.teavm.jso.dom.events.MouseEvent;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Position;
import areca.ui.component.Property;
import areca.ui.html.HtmlEventTarget.HtmlMouseEvent;

/**
 *
 * @author Falko Bräutigam
 */
public class HtmlMouseEventImpl {

    private static final Log log = LogFactory.getLog( HtmlMouseEventImpl.class );

    public static HtmlMouseEvent create( MouseEvent delegate ) {
        var elm = new HtmlMouseEvent();
        elm.clientPosition = Property.create( elm, "clientPosition",
                () -> Position.of( delegate.getClientX(), delegate.getClientY() ) );
        return elm;
    }

}
