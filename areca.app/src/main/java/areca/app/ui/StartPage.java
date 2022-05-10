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

import static areca.ui.component2.Events.EventType.SELECT;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.Button;
import areca.ui.component2.Text;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.layout.RasterLayout;
import areca.ui.pageflow.Page;
import areca.ui.pageflow.PageUIComposite;
import areca.ui.pageflow.Pageflow;

/**
 *
 * @author Falko Bräutigam
 */
public class StartPage
        extends Page {

    private static final Log LOG = LogFactory.getLog( StartPage.class );

    private PageUIComposite     ui;

    @Override
    protected UIComponent doInit( UIComposite parent ) {
        ui = new PageUIComposite( parent );
        ui.header.add( new Text(), title -> title.content.set( "StartPage" ) );

        ui.body.layout.set( new RasterLayout() {{spacing.set( 10 );}} );

        // Contacts
        ui.body.add( new Button(), btn -> {
            btn.label.set( "C²" );
            //btn.htmlElm.styles.set( "border-radius", "9px" );
            btn.events.on( SELECT, ev ->  {
                Pageflow.current().open( new ContactsPage(), StartPage.this, ev.clientPos() );
            });
        });

        // Close
        ui.body.add( new Button(), btn -> {
            btn.label.set( "X" );
            btn.events.on( SELECT, ev -> {
                Pageflow.current().close( StartPage.this );
            });
        });
        return ui;
    }

    @Override
    protected void doDispose() {
        LOG.info( "DISPOSING..." );
    }
}
