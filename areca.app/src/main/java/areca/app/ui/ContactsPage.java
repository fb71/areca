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

import areca.app.ArecaApp;
import areca.app.model.Contact;
import areca.app.service.carddav.CardDavTest;
import areca.app.service.carddav.CarddavSynchronizer;
import areca.common.NullProgressMonitor;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Size;
import areca.ui.component2.Button;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.layout.RowLayout;
import areca.ui.pageflow.Page;
import areca.ui.pageflow.PageContainer;
import areca.ui.pageflow.Pageflow;

/**
 *
 * @author Falko Bräutigam
 */
public class ContactsPage extends Page {

    private static final Log LOG = LogFactory.getLog( ContactsPage.class );

    private PageContainer ui;


    @Override
    protected UIComponent doInit( UIComposite parent ) {
        ui = new PageContainer( this, parent );
        ui.title.set( "Contacts" );
        ui.body.layout.set( new RowLayout() {{spacing.set( 5 ); margins.set( Size.of( 10, 10 ) );}} );

        fetchContacts();

        ui.body.add( new Button(), btn -> {
            btn.label.set( "^" );
            btn.events.on( SELECT, ev -> syncContacts() );
        });

        ui.body.add( new Button(), btn -> {
            btn.label.set( "XX" );
            btn.events.on( SELECT, ev -> Pageflow.current().close( ContactsPage.this ) );
        });
        ui.body.layout();
        return ui;
    }


    protected void fetchContacts() {
        ui.body.components.disposeAll();
        ArecaApp.instance().unitOfWork()
                .query( Contact.class )
                .execute()
                .onSuccess( opt -> opt.ifPresent( contact -> {
                    ui.body.add( new Button(), btn -> makeContactButton( btn, contact ) );
                    ui.body.layout();
                }));
    }


    protected void syncContacts() {
        var s = new CarddavSynchronizer( CardDavTest.ARECA_CONTACTS_ROOT, ArecaApp.instance().repo() );
        s.monitor.set( new NullProgressMonitor() );
        s.start().onSuccess( contacts -> {
            LOG.info( "Contacts: %s", contacts.size() );
            fetchContacts();
        });
    }


    protected void makeContactButton( Button btn, Contact contact ) {
        btn.label.set( contact.firstname.get() );
        btn.events.on( SELECT, ev -> {
            Pageflow.current().open( new ContactPage(), ContactsPage.this, ev.clientPos() );
        });
    }


    @Override
    protected void doDispose() {
    }

}
