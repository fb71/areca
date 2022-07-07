/*
 * Copyright (C) 2021-2022, the @authors. All rights reserved.
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
import areca.ui.component2.Events.UIEvent;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
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
        ui.title.set( "Anchors" );

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
                ArecaApp.current().forceIncrementalSync( 0 );
            });
        }});
        // Settings
        site.actions.add( new Action() {{
            icon.set( "settings" );
            description.set( "Open settings" );
            handler.set( (UIEvent ev) -> {
                site.pageflow().open( new SettingsPage(), StartPage.this, ev.clientPos() );
            });
        }});

        new AnchorsCloudPage( ui.body, this );

        return ui;
    }


    /* Just testing */
    PageSite site() {
        return site;
    }


    @Override
    protected void doDispose() {
        LOG.info( "DISPOSING..." );
    }
}
