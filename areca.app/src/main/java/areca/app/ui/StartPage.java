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

import areca.app.ArecaApp;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Action;
import areca.ui.Size;
import areca.ui.component2.Events.UIEvent;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.layout.RowLayout;
import areca.ui.pageflow.Page;
import areca.ui.pageflow.PageContainer;

/**
 *
 * @author Falko Bräutigam
 */
public class StartPage
        extends Page {

    private static final Log LOG = LogFactory.getLog( StartPage.class );

    private PageContainer     ui;

    @Override
    protected UIComponent doInit( UIComposite parent ) {
        ui = new PageContainer( this, parent );
        ui.title.set( "Start" );

        // Contacts
        site.actions.add( new Action() {{
            icon.set( "face" );
            description.set( "Show contacts" );
            handler.set( ev ->  {
                site.pageflow().open( new ContactsPage(), StartPage.this, ev.clientPos() );
            });
        }});
        // Sync
        site.actions.add( new Action() {{
            icon.set( "sync" );
            description.set( "Start synchronization" );
            handler.set( (UIEvent ev) -> {
                ArecaApp.instance().startGlobalServicesSync();
            });
        }});
        // Settings
        site.actions.add( new Action() {{
            icon.set( "settings" );
            description.set( "Open global settings" );
            handler.set( (UIEvent ev) -> {
                site.pageflow().open( new SettingsPage(), StartPage.this, ev.clientPos() );
            });
        }});

        ui.body.layout.set( new RowLayout() {{spacing.set( 5 ); margins.set( Size.of( 10, 10 ) );}} );

        return ui;
    }

    @Override
    protected void doDispose() {
        LOG.info( "DISPOSING..." );
    }
}