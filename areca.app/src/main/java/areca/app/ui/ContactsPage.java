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
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.ArrayList;

import org.polymap.model2.Property;
import org.polymap.model2.query.Query.Order;

import areca.app.ArecaApp;
import areca.app.model.Contact;
import areca.common.MutableInt;
import areca.common.Timer;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Action;
import areca.ui.Size;
import areca.ui.component2.Button;
import areca.ui.component2.ScrollableComposite;
import areca.ui.component2.Text;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.layout.FillLayout;
import areca.ui.layout.RasterLayout;
import areca.ui.pageflow.Page;
import areca.ui.pageflow.PageContainer;

/**
 *
 * @author Falko Bräutigam
 */
public class ContactsPage extends Page {

    private static final Log LOG = LogFactory.getLog( ContactsPage.class );

    private PageContainer       ui;

    protected long              timeout = 150;  // 300ms timeout before page animation starts

    protected Property<String>  orderBy = Contact.TYPE.firstname;

    protected ScrollableComposite body;


    @Override
    protected UIComponent doInit( UIComposite parent ) {
        ui = new PageContainer( this, parent );
        ui.title.set( "Contacts" );

        // order-by
        pageSite.actions.add( new Action() {{
            icon.set( "sort_by_alpha" );
            description.set( "Order by first- or lastname" );
            handler.set( ev ->  {
                orderBy = orderBy == Contact.TYPE.lastname ? Contact.TYPE.firstname : Contact.TYPE.lastname;
                LOG.info( "Order by: %s", orderBy );
                fetchContacts();
            });
        }});

        ui.body.layout.set( new FillLayout() );
        body = ui.body.add( new ScrollableComposite() {{
            layout.set( new RasterLayout() {{
                spacing.set( 10 );
                margins.set( Size.of( 10, 10 ) );
                itemSize.set( Size.of( 80, 75 ) );
            }});
        }});

        fetchContacts();
        return ui;
    }


    protected void fetchContacts() {
        body.components.disposeAll();
        body.add( new Text().content.set( "Loading..." ) );
        body.layout();

        var timer = Timer.start();
        var chunk = new ArrayList<Button>();
        var c  = new MutableInt( 0 );

        ArecaApp.instance().unitOfWork()
                .query( Contact.class )
                .orderBy( orderBy, Order.ASC )
                .execute()
                .onSuccess( (ctx,result) -> {
                    result.ifPresent( contact -> {
                        chunk.add( createContactButton( contact ) );
                    });
                    if (timer.elapsed( MILLISECONDS ) > timeout || ctx.isComplete()) {
                        LOG.info( "" + timer.elapsedHumanReadable() );
                        timer.restart();
                        timeout = 1000;

                        if (c.getAndIncrement() == 0) {
                            body.components.disposeAll();
                        }
                        chunk.forEach( btn -> body.add( btn ) );
                        chunk.clear();
                        body.layout();
                    }
                });
    }


    protected Button createContactButton( Contact contact ) {
        var btn = new Button();
        btn.cssClasses.add( "ContactButton" );
        if (contact.photo.opt().isPresent()) {
            btn.bgImage.set( contact.photo.get() );
        }
        else {
            btn.label.set( String.format( "%.7s %.7s",
                    contact.firstname.opt().orElse( "" ),
                    contact.lastname.opt().orElse( "" )) );
        }
        btn.tooltip.set( contact.label() );

        btn.events.on( SELECT, ev -> {
            //site.put( contact );
            pageSite.openPage( new ContactPage( contact ), ev.clientPos() );
        });
        return btn;
    }

}
