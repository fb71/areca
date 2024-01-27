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

import areca.app.model.SmtpSettings;
import areca.app.service.mail.MessageSendRequest;
import areca.common.Promise;
import areca.common.base.Supplier.RSupplier;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.form.Form;
import areca.ui.viewer.TextFieldViewer;
import areca.ui.viewer.transform.Number2StringTransform;

/**
 *
 * @author Falko Bräutigam
 */
public class SmtpSettingsPage
        extends ServiceSettingsPage<SmtpSettings> {

    private static final Log LOG = LogFactory.getLog( SmtpSettingsPage.class );


    public SmtpSettingsPage() {
        super( SmtpSettings.class );
    }


    @Override
    protected UIComponent onCreateUI( UIComposite parent ) {
        var result = super.onCreateUI( parent );
        ui.title.set( "Email Send Settings" );
        return result;
    }


    @Override
    protected SmtpSettings newSettings() {
        return uow.waitForResult().get().createEntity( SmtpSettings.class, proto -> {
            proto.host.set( "mail.polymap.de" );
            proto.port.set( 465 );
            //proto.from.set( CardDavTest.ARECA_USERNAME );
            proto.from.set( String.format( "%s%s%s", "areca", "@", "polymap.de" ) );
            proto.username.set( proto.from.get() );
            proto.pwd.set( "dienstag" );
        });
    }


    protected Promise<?> checkSettings( SmtpSettings settings ) {
        var out = MessageSendRequest.Message.create();
        out.setSubject( "Checking SMTP settings" );
        out.setText( "This is a test message to check your SMTP settings. Everything seems to be ok :) You may delete this message." );
        out.setTo( settings.from.get() );
        out.setFrom( settings.from.get() );

        var request = new MessageSendRequest( settings.toRequestParams(), out );
        return request.submit()
                .onError( e -> {
                    LOG.warn( e.toString(), e );
                });
    }

    @Override
    protected UIComposite buildCheckingForm( RSupplier<SmtpSettings> settings, Form form ) {
        return new SmtpCheckingForm( settings, form );
    }


    /**
     *
     */
    protected class SmtpCheckingForm
            extends CheckingForm {

        public SmtpCheckingForm( RSupplier<SmtpSettings> settings, Form form ) {
            super( settings, form );
        }

        @Override
        protected void buildForm() {
            add( form.newField().label( "Email Address" )
                    .viewer( new TextFieldViewer() )
                    .model( new PropertyAdapter<>( () -> settings.get().from ) )
                    .create() );
            add( form.newField().label( "Username" )
                    .viewer( new TextFieldViewer() )
                    .model( new PropertyAdapter<>( () -> settings.get().username ) )
                    .create() );
            add( form.newField().label( "Password" )
                    .viewer( new TextFieldViewer() )
                    .model( new PropertyAdapter<>( () -> settings.get().pwd ) )
                    .create() );
            add( form.newField().label( "Host" )
                    .viewer( new TextFieldViewer() )
                    .model( new PropertyAdapter<>( () -> settings.get().host ) )
                    .create() );
            add( form.newField().label( "Port" )
                    .viewer( new TextFieldViewer() )
                    .model( new Number2StringTransform(
                            new PropertyAdapter<Integer>( () -> settings.get().port ) ) )
                    .create() );
        }

        @Override
        protected void buildButtons( UIComposite container ) {
            container.add( createCheckButton( () -> checkSettings( settings.get() ) ) );
            container.add( createSubmitButton() );
        }
    }

}
