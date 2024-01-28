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

import areca.app.model.CarddavSettings;
import areca.app.service.carddav.CarddavService;
import areca.app.service.carddav.CarddavTest;
import areca.app.service.carddav.DavResource;
import areca.app.service.carddav.GetResourceRequest;
import areca.app.service.carddav.PropfindRequest;
import areca.app.service.carddav.VCard;
import areca.common.Platform;
import areca.common.Promise;
import areca.common.base.Supplier.RSupplier;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.viewer.TextFieldViewer;
import areca.ui.viewer.form.Form;

/**
 *
 * @author Falko Bräutigam
 */
public class CarddavSettingsPage
        extends ServiceSettingsPage<CarddavSettings> {

    private static final Log LOG = LogFactory.getLog( CarddavSettingsPage.class );

    public CarddavSettingsPage() {
        super( CarddavSettings.class );
    }


    @Override
    protected UIComponent onCreateUI( UIComposite parent ) {
        var result = super.onCreateUI( parent );
        ui.title.set( "CardDav Settings" );
        return result;
    }


    @Override
    protected CarddavSettings newSettings() {
        return uow.waitForResult().get().createEntity( CarddavSettings.class, proto -> {
            proto.url.set( CarddavTest.ARECA_CONTACTS_ROOT.url() );
            proto.username.set( CarddavTest.ARECA_USERNAME );
            proto.pwd.set( "..." );
        });
    }


    protected Promise<?> checkSettings( CarddavSettings settings ) {
        return Platform
                // just catch Exceptions inside the Promise
                .async( () -> {
                    var matcher = CarddavService.URL.matcher( settings.url.get() );
                    if (!matcher.matches()) {
                        throw new RuntimeException( "URL is not valid: " + settings.url.get() );
                    }
                    LOG.debug( "%s / %s", matcher.group( 1 ), matcher.group( 2 ) );
                    return new PropfindRequest( DavResource.create( matcher.group( 1 ), matcher.group( 2 ) )
                            .auth( settings.username.get(), settings.pwd.get() ) );
                })
                .then( propfind -> propfind.submit() )
                .then( res -> new GetResourceRequest( res[0] ).submit() )
                .map( res -> VCard.parse( res.text() ) )
                .onSuccess( vcard -> LOG.info( "VCard: %s", vcard ) );
    }


    @Override
    protected UIComposite buildCheckingForm( RSupplier<CarddavSettings> settings, Form form ) {
        return new CarddavCheckingForm( settings, form );
    }


    /**
     *
     */
    protected class CarddavCheckingForm
            extends CheckingForm {

        public CarddavCheckingForm( RSupplier<CarddavSettings> settings, Form form ) {
            super( settings, form );
        }

        @Override
        protected void buildForm() {
            add( form.newField().label( "URL" )
                    .viewer( new TextFieldViewer() )
                    .model( new PropertyAdapter<>( () -> settings.get().url ) )
                    //.validator( URL... )
                    .create() );
            add( form.newField().label( "Username" )
                    .viewer( new TextFieldViewer() )
                    .model( new PropertyAdapter<>( () -> settings.get().username ) )
                    .create() );
            add( form.newField().label( "Password" )
                    .viewer( new TextFieldViewer() )
                    .model( new PropertyAdapter<>( () -> settings.get().pwd ) )
                    .create() );
        }

        @Override
        protected void buildButtons( UIComposite container ) {
            container.add( createCheckButton( () -> checkSettings( settings.get() ) ) );
            container.add( createSubmitButton() );
        }
    }

}
