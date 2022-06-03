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

import areca.app.model.ImapSettings;
import areca.app.service.carddav.CardDavTest;
import areca.app.service.imap.FolderListCommand;
import areca.app.service.imap.ImapRequest;
import areca.app.service.imap.ImapRequest.LoginCommand;
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
public class ImapSettingsPage2
        extends ServiceSettingsPage<ImapSettings> {

    private static final Log LOG = LogFactory.getLog( ImapSettingsPage2.class );


    public ImapSettingsPage2() {
        super( ImapSettings.class );
    }


    @Override
    protected UIComponent doInit( UIComposite parent ) {
        var result = super.doInit( parent );
        ui.title.set( "Email Settings" );
        return result;
    }


    @Override
    protected ImapSettings newSettings() {
        return uow.waitForResult().get().createEntity( ImapSettings.class, proto -> {
            proto.host.set( "mail.polymap.de" );
            proto.port.set( 993 );
            //proto.username.set( String.format( "%s%s%s", "falko", "@", "polymap.de" ) );
            proto.username.set( CardDavTest.ARECA_USERNAME );
            proto.pwd.set( "..." );
        });
    }


    protected Promise<?> checkSettings( ImapSettings settings ) {
        var request = new ImapRequest( self -> {
            self.host = settings.host.get();
            self.port = settings.port.get();
            self.loginCommand = new LoginCommand( settings.username.get(), settings.pwd.get() );
            self.commands.add( new FolderListCommand() );
        });
        return request.submit();
    }


    @Override
    protected UIComposite buildCheckingForm( ImapSettings settings, Form form ) {
        return new ImapCheckingForm( settings, form );
    }


    /**
     *
     */
    protected class ImapCheckingForm
            extends CheckingForm {

        public ImapCheckingForm( ImapSettings settings, Form form ) {
            super( settings, form );
        }

        @Override
        protected void buildForm() {
            add( form.newField().label( "Username" )
                    .viewer( new TextFieldViewer() )
                    .adapter( new PropertyAdapter<>( () -> settings.username ) )
                    .create() );
            add( form.newField().label( "Password" )
                    .viewer( new TextFieldViewer() )
                    .adapter( new PropertyAdapter<>( () -> settings.pwd ) )
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
        }

        @Override
        protected void buildButtons( UIComposite container ) {
            container.add( createCheckButton( () -> checkSettings( settings ) ) );
            container.add( createSubmitButton() );
        }
    }

}
