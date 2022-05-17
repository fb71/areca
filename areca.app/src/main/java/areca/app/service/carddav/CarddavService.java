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
package areca.app.service.carddav;

import areca.app.ArecaApp;
import areca.app.service.Service;
import areca.app.service.SyncableService;
import areca.common.ProgressMonitor;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public class CarddavService
        extends Service
        implements SyncableService {

    private static final Log LOG = LogFactory.getLog( CarddavService.class );


    @Override
    public String label() {
        return "Contacts - Carddav: ???";
    }


    @Override
    public Sync newSync( ProgressMonitor monitor ) {
        return new Sync() {

            @Override
            public void start() {
                var synchronizer = new CarddavSynchronizer( CardDavTest.ARECA_CONTACTS_ROOT, ArecaApp.instance().repo() );
                synchronizer.monitor.set( monitor );
                synchronizer.start()
                        .onSuccess( contacts -> LOG.info( "Contacts: %s", contacts.size() ) )
                        .onError( ArecaApp.instance().defaultErrorHandler() );
            }
        };
    }

}
