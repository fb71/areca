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
package areca.app.ui;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component.Text;
import areca.ui.component.UIComponent;
import areca.ui.component.UIComposite;
import areca.ui.pageflow.Page;
import areca.ui.pageflow.PageUIComposite;

/**
 *
 * @author Falko Bräutigam
 */
public class ContactPage extends Page {

    private static final Log LOG = LogFactory.getLog( ContactPage.class );

    private PageUIComposite ui;

    @Override
    protected UIComponent doInit( UIComposite parent ) {
        ui = new PageUIComposite( parent );
        ui.header.add( new Text(), title -> title.text.set( "Contacts" ) );

        ui.body.add( new Text(), t -> t.text.set( "Contact" ) );

        ui.body.layout();
        return ui;
    }

    @Override
    protected void doDispose() {
    }

}
