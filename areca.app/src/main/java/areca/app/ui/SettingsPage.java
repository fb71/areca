/*
 * Copyright (C) 2022, the @authors. All rights reserved.
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

import static areca.ui.Orientation.VERTICAL;

import org.teavm.jso.browser.Location;
import org.teavm.jso.browser.Window;

import areca.common.base.Supplier.RSupplier;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Size;
import areca.ui.component2.Button;
import areca.ui.component2.Events.EventType;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.layout.RowConstraints;
import areca.ui.layout.RowLayout;
import areca.ui.pageflow.Page;
import areca.ui.pageflow.PageContainer;

/**
 *
 * @author Falko Bräutigam
 */
public class SettingsPage
        extends Page {

    private static final Log LOG = LogFactory.getLog( SettingsPage.class );

    private PageContainer     ui;


    @Override
    protected UIComponent onCreateUI( UIComposite parent ) {
        ui = new PageContainer( this, parent );
        ui.title.set( "Settings" );

        ui.body.layout.set( new RowLayout().orientation.set( VERTICAL ).margins.set( Size.of( 10, 10 ) ).spacing.set( 8 ).fillWidth.set( true ) );
        ui.body.add( createBtn( "Mail / IMAP", "", "email", () -> new ImapSettingsPage2() ) );
        ui.body.add( createBtn( "Mail Send / SMTP", "", "send", () -> new SmtpSettingsPage() ) );
        ui.body.add( createBtn( "Matrix", "", "chat", () -> new MatrixSettingsPage2() ) );
        ui.body.add( createBtn( "Contacts / CardDav", "", "faces", () -> new CarddavSettingsPage() ) );

        ui.body.add( createBtn( "Tests", "", "cruelty_free", () -> new InternalTestsPage() ) );

        ui.body.add( new Button() {{
            label.set( "RELOAD" );
            events.on( EventType.SELECT, ev -> {
                LOG.info( "Location: %s", Window.current().getLocation().getPathName() );
                Location.current().setSearch( "v=" + (byte)System.currentTimeMillis() );
            });
        }});
        return ui;
    }


    protected UIComponent createBtn( String _label, String _tooltip, String _icon, RSupplier<Page> _pageFactory ) {
        return new Button() {{
            label.set( _label );
            //icon.set( _icon );
            tooltip.set( _tooltip );
            layoutConstraints.set( new RowConstraints().height.set( 50 ) );
            events.on( EventType.SELECT, ev -> {
                pageSite.createPage( _pageFactory.supply() ).origin( ev.clientPos() ).open();
            });
        }};
    }

}
