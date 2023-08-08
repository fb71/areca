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

import static areca.ui.Orientation.VERTICAL;

import org.apache.commons.lang3.StringUtils;

import areca.app.model.Contact;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Size;
import areca.ui.component2.Button;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.form.Form;
import areca.ui.layout.RowConstraints;
import areca.ui.layout.RowLayout;
import areca.ui.pageflow.Page;
import areca.ui.pageflow.PageContainer;
import areca.ui.viewer.TextFieldViewer;

/**
 *
 * @author Falko Bräutigam
 */
public class ContactPage extends Page {

    private static final Log LOG = LogFactory.getLog( ContactPage.class );

    protected PageContainer ui;

    protected Contact       contact;


    public ContactPage( Contact contact ) {
        this.contact = contact;
    }


    @Override
    protected UIComponent onCreateUI( UIComposite parent ) {
        ui = new PageContainer( this, parent );
        ui.title.set( StringUtils.abbreviate( contact.label(), 25 ) );
        ui.body.layout.set( new RowLayout() {{orientation.set( VERTICAL ); fillWidth.set( true ); spacing.set( 15 ); margins.set( Size.of( 10, 10 ) );}} );

        ui.body.add( new Button() {{
            layoutConstraints.set( new RowConstraints().height.set( 80 ) );
            if (contact.photo.get() != null) {
                image.set( contact.photo.get() );
            }
            else {
                icon.set( "face" );
            }
        }});

        // form
        ui.body.add( new UIComposite() {{
            layoutConstraints.set( new RowConstraints() {{height.set( 200 );}} );
            layout.set( new RowLayout().orientation.set( VERTICAL ).fillWidth.set( true )
                    .spacing.set( 25 ).margins.set( Size.of( 10, 15 ) ) );

            var form = new Form();
            add( form.newField().label( "First" )
                    .viewer( new TextFieldViewer() )
                    .adapter( new PropertyAdapter<>( () -> contact.firstname ) )
                    .create() );
            add( form.newField().label( "Last" )
                    .viewer( new TextFieldViewer() )
                    .adapter( new PropertyAdapter<>( () -> contact.lastname ) )
                    .create() );
            add( form.newField().label( "Email" )
                    .viewer( new TextFieldViewer() )
                    .adapter( new PropertyAdapter<>( () -> contact.email ) )
                    .create() );
            add( form.newField().label( "Phone" )
                    .viewer( new TextFieldViewer() )
                    .adapter( new PropertyAdapter<>( () -> contact.phone ) )
                    .create() );
        }});

//        // form magic :)
//        ui.body.add( new UIComposite() {{
//            layoutConstraints.set( new RowConstraints() {{height.set( 200 );}} );
//            layout.set( new RowLayout() {{orientation.set( VERTICAL ); fillWidth.set( true ); spacing.set( 10 ); margins.set( Size.of( 5, 5 ) );}} );
//            bordered.set( true );
//
//            var form = new ContactForm( contact );
//            new FormRenderer( form ).render( this );
//        }});

        return ui;
    }

    @Override
    protected void onDispose() {
    }
}
