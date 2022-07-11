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
package areca.app;

import java.util.Arrays;
import java.util.List;

import org.polymap.model2.Entity;

import areca.app.model.Address;
import areca.app.service.Service;
import areca.app.service.TransportService;
import areca.app.service.TransportService.Transport;
import areca.app.service.TransportService.TransportContext;
import areca.app.service.carddav.CarddavService;
import areca.app.service.mail.MailService;
import areca.app.service.matrix.MatrixService;
import areca.common.ProgressMonitor;
import areca.common.Promise;
import areca.common.base.Consumer;
import areca.common.base.Opt;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public class Services {

    private static final Log LOG = LogFactory.getLog( Services.class );

    private static List<? extends Service> SERVICES = Arrays.asList(
            new CarddavService(),
            new MailService(),
            new MatrixService());


    // instance *******************************************

    private ArecaApp        app;


    public Services( ArecaApp app ) {
        this.app = app;
    }


    @SuppressWarnings("unchecked")
    public <R> Sequence<R,RuntimeException> ofType( Class<R> type ) {
        return Sequence.of( SERVICES ).filter( type::isInstance ).map( s -> (R)s );
    }


    /**
     * One Transport that can be used for the given receipient, or {@link Promise#absent()}
     * if there is no Transport that can handle the receipient.
     */
    @SuppressWarnings("unchecked")
    public void transportFor( Address receipient, Consumer<Transport,Exception> work ) {
        LOG.info( "Transports for: %s ", receipient );
        var services = ofType( TransportService.class ).toList();
        for (var service : services) {
            var uow = app.settingsRepo.newUnitOfWork();
            uow.query( (Class<Entity>)service.transportSettingsType() )
                    .executeCollect()
                    .onSuccess( settingss -> {
                        for (var settings : settingss) {

                            var ctx = new TransportContext() {
                                ProgressMonitor monitor;
                                @Override public ProgressMonitor newMonitor() {
                                    return monitor != null ? monitor : (monitor = app.newAsyncOperation());
                                }
                            };

                            Opt<Transport> opt = service.newTransport( receipient, ctx, settings );
                            opt.ifPresent( transport -> {
                                work.accept( transport );
                            });
                        }
                    })
                    //.onSuccess( __ -> LOG.info( "Transport done." ) )
                    .onError( app.defaultErrorHandler() );
        }
    }

}
