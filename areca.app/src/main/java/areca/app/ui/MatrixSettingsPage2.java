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

import areca.app.ArecaApp;
import areca.app.model.MatrixSettings;
import areca.app.service.matrix.MatrixClient;
import areca.common.Promise;
import areca.common.Promise.Completable;
import areca.common.base.Supplier.RSupplier;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.form.Form;
import areca.ui.viewer.TextFieldViewer;

/**
 *
 * @author Falko Bräutigam
 */
public class MatrixSettingsPage2
        extends ServiceSettingsPage<MatrixSettings> {

    private static final Log LOG = LogFactory.getLog( MatrixSettingsPage2.class );

    protected SingleValue<String>       password = new SingleValue<>( "" );


    public MatrixSettingsPage2() {
        super( MatrixSettings.class );
    }


    @Override
    protected UIComponent onCreateUI( UIComposite parent ) {
        var result = super.onCreateUI( parent );
        ui.title.set( "Matrix Settings" );
        return result;
    }


    @Override
    protected MatrixSettings newSettings() {
        return uow.waitForResult().get().createEntity( MatrixSettings.class, proto -> {
            proto.baseUrl.set( "https://matrix.fulda.social" );
            proto.username.set( "@bolo:fulda.social" );
            //proto.accessToken.set( "..." );
        });
    }


    protected Promise<?> checkSettings( MatrixSettings settings ) {
        Completable<?> result = new Completable();
        var matrix = MatrixClient.create( ArecaApp.proxiedUrl( settings.baseUrl.get() ) );
        matrix.loginWithPassword( settings.username.get(), password.getValue() )
                .then( credentials -> {
                    LOG.info( "Logged in: %s", credentials.userd() );
                    settings.accessToken.set( credentials.accessToken() );
                    settings.deviceId.set( credentials.deviceId() );
                    result.complete( null );
                })
                .catch_( err -> {
                    result.completeWithError( new Exception( "Error while login." ) );
                });
        return result;
    }


    @Override
    protected UIComposite buildCheckingForm( RSupplier<MatrixSettings> settings, Form form ) {
        return new MatrixCheckingForm( settings, form );
    }


    /**
     *
     */
    protected class MatrixCheckingForm
            extends CheckingForm {

        public MatrixCheckingForm( RSupplier<MatrixSettings> settings, Form form ) {
            super( settings, form );
        }

        @Override
        protected void buildForm() {
            add( form.newField().label( "Base URL" )
                    .viewer( new TextFieldViewer() )
                    .adapter( new PropertyAdapter<>( () -> settings.get().baseUrl ) )
                    .create() );
            add( form.newField().label( "Username" )
                    .viewer( new TextFieldViewer() )
                    .adapter( new PropertyAdapter<>( () -> settings.get().username ) )
                    .create() );
            add( form.newField().label( "Password" )
                    .viewer( new TextFieldViewer() )
                    .adapter( password )
                    .create() );
        }

        @Override
        protected void buildButtons( UIComposite container ) {
            container.add( createCheckButton( () -> checkSettings( settings.get() ) ) );
            container.add( createSubmitButton() );
        }
    }

}
