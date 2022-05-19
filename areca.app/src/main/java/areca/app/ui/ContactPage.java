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

import areca.app.model.Contact;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Size;
import areca.ui.component2.Text;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.form.Form;
import areca.ui.form.FormRenderer;
import areca.ui.layout.RowConstraints;
import areca.ui.layout.RowLayout;
import areca.ui.pageflow.Page;
import areca.ui.pageflow.PageContainer;
import areca.ui.viewer.TextViewer;

/**
 *
 * @author Falko Bräutigam
 */
public class ContactPage extends Page {

    private static final Log LOG = LogFactory.getLog( ContactPage.class );

    private PageContainer ui;


    @Override
    protected UIComponent doInit( UIComposite parent ) {
        ui = new PageContainer( this, parent );
        ui.title.set( "Contact" );
        ui.body.layout.set( new RowLayout() {{orientation.set( VERTICAL ); fillWidth.set( true ); spacing.set( 15 ); margins.set( Size.of( 10, 10 ) );}} );

        var contact = site.data( Contact.class );
        ui.body.add( new Text() {{
            layoutConstraints.set( new RowConstraints() {{height.set( 20 ); }} );
            content.set( "Contact: " + contact.label() );
        }});

        // form
        ui.body.add( new UIComposite() {{
            layoutConstraints.set( new RowConstraints() {{height.set( 200 );}} );
            layout.set( new RowLayout() {{orientation.set( VERTICAL ); fillWidth.set( true ); spacing.set( 10 ); margins.set( Size.of( 5, 5 ) );}} );
            bordered.set( true );

            var form = new Form();
            add( form.newField().label( "Firstname" )
                    .viewer( new TextViewer() )
                    .adapter( new PropertyAdapter<>( () -> contact.firstname ) )
                    .create() );
            add( form.newField().label( "Lastname" )
                    .viewer( new TextViewer() )
                    .adapter( new PropertyAdapter<>( () -> contact.lastname ) )
                    .create() );
        }});

        // form magic :)
        ui.body.add( new UIComposite() {{
            layoutConstraints.set( new RowConstraints() {{height.set( 200 );}} );
            layout.set( new RowLayout() {{orientation.set( VERTICAL ); fillWidth.set( true ); spacing.set( 10 ); margins.set( Size.of( 5, 5 ) );}} );
            bordered.set( true );

            var form = new ContactForm( contact );
            new FormRenderer( form ).render( this );
        }});

        ui.body.layout();
        return ui;
    }

    @Override
    protected void doDispose() {
    }
}
