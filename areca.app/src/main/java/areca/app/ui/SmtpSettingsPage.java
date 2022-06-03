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
import areca.app.service.smtp.SmtpRequest;
import areca.app.service.smtp.SmtpRequest.AuthPlainCommand;
import areca.app.service.smtp.SmtpRequest.HeloCommand;
import areca.app.service.smtp.SmtpRequest.MailFromCommand;
import areca.app.service.smtp.SmtpRequest.QuitCommand;
import areca.common.Promise;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.form.Form;
import areca.ui.viewer.Number2StringTransformer;
import areca.ui.viewer.TextFieldViewer;

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
    protected UIComponent doInit( UIComposite parent ) {
        var result = super.doInit( parent );
        ui.title.set( "Email Send Settings" );
        return result;
    }


    @Override
    protected SmtpSettings newSettings() {
        return uow.waitForResult().get().createEntity( SmtpSettings.class, proto -> {
            proto.host.set( "mail.polymap.de" );
            proto.port.set( 465 );
            //proto.from.set( CardDavTest.ARECA_USERNAME );
            proto.from.set( String.format( "%s%s%s", "falko", "@", "polymap.de" ) );
            proto.username.set( proto.from.get() );
            proto.pwd.set( "..." );
        });
    }


    protected Promise<?> checkSettings( SmtpSettings settings ) {
        var request = new SmtpRequest( self -> {
            self.host = settings.host.get();
            self.port = settings.port.get();
            self.loginCommand = new HeloCommand( "zuhause" ); // XXX
            self.commands.add( new AuthPlainCommand( settings.username.get(), settings.pwd.get() ) );
            self.commands.add( new MailFromCommand( settings.from.get() ) );
            self.commands.add( new QuitCommand() );
        });
        return request.submit()
                .onSuccess( command -> {
                    LOG.info( "Response: %s", command );
                });
    }

    @Override
    protected UIComposite buildCheckingForm( SmtpSettings settings, Form form ) {
        return new SmtpCheckingForm( settings, form );
    }


    /**
     *
     */
    protected class SmtpCheckingForm
            extends CheckingForm {

        public SmtpCheckingForm( SmtpSettings settings, Form form ) {
            super( settings, form );
        }

        @Override
        protected void buildForm() {
            add( form.newField().label( "Email Address" )
                    .viewer( new TextFieldViewer() )
                    .adapter( new PropertyAdapter<>( () -> settings.from ) )
                    .create() );
            add( form.newField().label( "Host" )
                    .viewer( new TextFieldViewer() )
                    .adapter( new PropertyAdapter<>( () -> settings.host ) )
                    .create() );
            add( form.newField().label( "Port" )
                    .viewer( new TextFieldViewer() )
                    .transformer( new Number2StringTransformer() )
                    .adapter( new PropertyAdapter<>( () -> settings.port ) )
                    .create() );
            add( form.newField().label( "Username" )
                    .viewer( new TextFieldViewer() )
                    .adapter( new PropertyAdapter<>( () -> settings.username ) )
                    .create() );
            add( form.newField().label( "Password" )
                    .viewer( new TextFieldViewer() )
                    .adapter( new PropertyAdapter<>( () -> settings.pwd ) )
                    .create() );
        }

        @Override
        protected void buildButtons( UIComposite container ) {
            container.add( createCheckButton( () -> checkSettings( settings ) ) );
            container.add( createSubmitButton() );
        }
    }

}
