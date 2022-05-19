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
import java.util.Collections;
import java.util.List;
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
        ui.body.layout.set( new PrioRasterLayout() );

        fetchContacts();

        ui.body.layout();
        return ui;
    }


    protected long lastLayout;

    protected long timeout = 280;  // 300ms timeout before page animation starts

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
                        timeout = 1000;

                        chunk.forEach( btn -> ui.body.add( btn ) );
                        chunk.clear();
                        ui.body.layout();
                    }
                });
    }


    protected Button makeContactButton( Contact contact ) {
        var btn = new Button();
        btn.cssClasses.add( "ContactButton" );
        btn.label.set( String.format( "%.7s %.7s",
                contact.firstname.opt().orElse( "" ),
                contact.lastname.opt().orElse( "" )) );
        btn.tooltip.set( contact.label() );
        btn.events.on( SELECT, ev -> {
            site.put( contact );
            site.pageflow().open( new ContactPage(), ContactsPage.this, ev.clientPos() );
        });
        return btn;
    }


    @Override
    protected void doDispose() {
    }


    /**
     *
     */
    class PrioRasterLayout
            extends RasterLayout {

        protected PrioRasterLayout() {
            itemSize.set( Size.of( 74, 68 ) );
            spacing.set( 5 );
            margins.set( Size.of( 5, 5 ) );
        }


        @Override
        public void layout( UIComposite composite ) {
            List<Button> sorted = composite.components.values().map( c -> (Button)c ).toList();
            Collections.sort( sorted, (b1,b2) -> StringUtils.compare( b1.label.value(), b2.label.value() ) );
            doLayout( composite, sorted );
        }
    }

}