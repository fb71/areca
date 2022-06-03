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
import areca.common.Promise;
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
        return "Carddav";
    }


    @Override
    public Promise<Sync> newSync( SyncType syncType, SyncContext ctx ) {
        if (syncType == SyncType.FULL) {
            var sync = new Sync() {
                @Override
                public Promise<?> start() {
                    var uow = ArecaApp.instance().repo().newUnitOfWork();
                    var synchronizer = new CarddavSynchronizer( CarddavTest.ARECA_CONTACTS_ROOT, uow );
                    synchronizer.monitor.set( ctx.monitor );
                    return synchronizer.start()
                            .onSuccess( contacts -> LOG.info( "Contacts: %s", contacts.size() ) );
                }
            };
            return Promise.completed( sync );
        }
        else {
            return SyncableService.super.newSync( syncType, ctx );
        }
    }

}
