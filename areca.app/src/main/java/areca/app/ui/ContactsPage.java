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

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

import areca.app.ArecaApp;
import areca.app.model.Contact;
import areca.common.Timer;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Size;
import areca.ui.component2.Button;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.layout.RasterLayout;
import areca.ui.pageflow.Page;
import areca.ui.pageflow.PageContainer;
import areca.ui.pageflow.Pageflow;

/**
 *
 * @author Falko Bräutigam
 */
public class ContactsPage extends Page {

    private static final Log LOG = LogFactory.getLog( ContactsPage.class );

    private PageContainer       ui;


    @Override
    protected UIComponent doInit( UIComposite parent ) {
        ui = new PageContainer( this, parent );
        ui.title.set( "Contacts" );
        ui.body.layout.set( new RasterLayout() {{itemSize.set( Size.of( 65, 65 ) ); spacing.set( 5 ); margins.set( Size.of( 5, 5 ) );}} );

        fetchContacts();

        ui.body.layout();
        return ui;
    }


    protected long lastLayout;

    protected long timeout = 280;

    protected void fetchContacts() {
        ui.body.components.disposeAll();
        lastLayout = System.currentTimeMillis();
        var timer = Timer.start();
        var chunk = new ArrayList<Button>();

        ArecaApp.instance().unitOfWork()
                .query( Contact.class )
                .execute()
                .onSuccess( (ctx,result) -> {
                    result.ifPresent( contact -> {
                        chunk.add( makeContactButton( contact ) );
                    });
                    var now = System.currentTimeMillis();
                    if (now > lastLayout + timeout || ctx.isComplete()) {
                        LOG.info( "" + timer.elapsedHumanReadable() );
                        timer.restart();
                        lastLayout = now;
                        timeout = 750;

                        chunk.forEach( btn -> ui.body.add( btn ) );
                        chunk.clear();
                        ui.body.layout();
                    }
                });
    }


    protected Button makeContactButton( Contact contact ) {
        var btn = new Button();
        btn.cssClasses.add( "ContactButton" );
        btn.label.set( StringUtils.left( contact.firstname.get(), 6 ) );
        btn.tooltip.set( contact.label() );
        btn.events.on( SELECT, ev -> {
            Pageflow.current().open( new ContactPage(), ContactsPage.this, ev.clientPos() );
        });
        return btn;
    }


    @Override
    protected void doDispose() {
    }

}
