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

import static areca.common.Scheduler.Priority.MAIN_EVENT_LOOP;

import areca.app.model.ImapSettings;
import areca.app.service.carddav.CarddavTest;
import areca.app.service.mail.AccountInfoRequest;
import areca.common.Promise;
import areca.common.base.Supplier.RSupplier;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.viewer.TextFieldViewer;
import areca.ui.viewer.form.Form;
import areca.ui.viewer.transform.Number2StringTransform;

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
    protected UIComponent onCreateUI( UIComposite parent ) {
        var result = super.onCreateUI( parent );
        ui.title.set( "Mail Settings" );
        return result;
    }


    @Override
    protected ImapSettings newSettings() {
        return uow.waitForResult().get().createEntity( ImapSettings.class, proto -> {
            proto.host.set( "mail.polymap.de" );
            proto.port.set( 993 );
            //proto.username.set( String.format( "%s%s%s", "falko", "@", "polymap.de" ) );
            proto.username.set( CarddavTest.ARECA_USERNAME );
            proto.pwd.set( "..." );
            proto.monthsToSync.set( 1 );
        });
    }


    protected Promise<?> checkSettings( ImapSettings settings ) {
        return new AccountInfoRequest( settings.toRequestParams( MAIN_EVENT_LOOP ) ).submit();
    }


    @Override
    protected UIComposite buildCheckingForm( RSupplier<ImapSettings> settings, Form form ) {
        return new ImapCheckingForm( settings, form );
    }


    /**
     *
     */
    protected class ImapCheckingForm
            extends CheckingForm {

        public ImapCheckingForm( RSupplier<ImapSettings> settings, Form form ) {
            super( settings, form );
        }

        @Override
        protected void buildForm() {
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
            add( form.newField().label( "Months to sync" )
                    .viewer( new TextFieldViewer() )
                    .model( new Number2StringTransform(
                            new PropertyAdapter<Integer>( () -> settings.get().monthsToSync ) ) )
                    .create() );
        }

        @Override
        protected void buildButtons( UIComposite container ) {
            container.add( createCheckButton( () -> checkSettings( settings.get() ) ) );
            container.add( createSubmitButton() );
        }
    }

}
