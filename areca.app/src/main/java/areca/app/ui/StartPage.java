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
    protected UIComponent onCreateUI( UIComposite parent ) {
        ui = new PageContainer( this, parent );
        ui.title.set( "Anchors" );

        // Contacts
        pageSite.actions.add( new Action() {{
            icon.set( "face" );
            description.set( "Show contacts" );
            handler.set( ev ->  {
                pageSite.createPage( new ContactsPage() ).origin( ev.clientPos() ).open();
            });
        }});
        // Sync
        pageSite.actions.add( new Action() {{
            icon.set( "sync" );
            description.set( "Start synchronization" );
            handler.set( (UIEvent ev) -> {
                ArecaApp.current().synchronization.triggerIncremental( 0 );
            });
        }});
        // Settings
        pageSite.actions.add( new Action() {{
            icon.set( "settings" );
            description.set( "Open settings" );
            handler.set( (UIEvent ev) -> {
                pageSite.createPage( new SettingsPage() ).origin( ev.clientPos() ).open();
            });
        }});

        new AnchorsCloudPage( ui.body, this );

        return ui;
    }


    /* Just testing */
    PageSite site() {
        return pageSite;
    }


    @Override
    protected void onDispose() {
        LOG.info( "DISPOSING..." );
    }
}
