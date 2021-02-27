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
package areca.app.ui;

import static areca.ui.controller.Context.Mode.IN_OUT;

import org.polymap.model2.Property;

import areca.app.model.Message;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.RuntimeInfo;
import areca.ui.controller.Context;
import areca.ui.controller.Context.Mode;
import areca.ui.controller.Controller;
import areca.ui.controller.UI;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class MessageController
        extends Controller {

    private static final Log log = LogFactory.getLog( MessageController.class );

    @Context(type = Message.class, mode = IN_OUT)
    protected Message           selected;

    @Context(type = String.class, mode = Mode.LOCAL, scope = "from")
    @UI(type = String.class, label = "Absender")
    protected Property<String>  from;

    @Context(type = String.class, mode = Mode.LOCAL, scope = "text")
    @UI(type = String.class)
    protected Property<String>  body;


    @Override
    protected void init( @SuppressWarnings("hiding") Site site ) {
        super.init( site );
        site.addContext( selected.from, Mode.LOCAL, "from" );
        site.addContext( selected.text, Mode.LOCAL, "text" );
    }

}
