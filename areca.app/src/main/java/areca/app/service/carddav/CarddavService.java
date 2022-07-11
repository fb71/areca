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

import static areca.app.service.SyncableService.SyncType.FULL;
import static areca.app.service.SyncableService.SyncType.INCREMENTAL;

import java.util.regex.Pattern;

import areca.app.model.CarddavSettings;
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
        implements SyncableService<CarddavSettings> {

    private static final Log LOG = LogFactory.getLog( CarddavService.class );

    public static final Pattern URL = Pattern.compile( "(https?://[^/]+)(/.+)?", Pattern.CASE_INSENSITIVE );

    @Override
    public String label() {
        return "CardDav";
    }


    @Override
    public Sync newSync( SyncType syncType, SyncContext ctx, CarddavSettings settings ) {
        LOG.info( "Sync: %s", syncType );
        if (syncType == FULL || syncType == INCREMENTAL) {
            return new Sync() {
                @Override
                public Promise<?> start() {
                    var matcher = URL.matcher( settings.url.get() );
                    if (!matcher.matches()) {
                        throw new RuntimeException( "URL is not valid: " + settings.url.get() );
                    }
                    var res = DavResource.create( matcher.group( 1 ), matcher.group( 2 ) )
                            .auth( settings.username.get(), settings.pwd.get() );
                    var synchronizer = new CarddavSynchronizer( res, ctx.unitOfWork() );
                    synchronizer.monitor.set( ctx.monitor() );
                    return synchronizer.start()
                            .onSuccess( contacts -> LOG.info( "Contacts: %s", contacts.size() ) );
                }
            };
        }
        else {
            return (Sync)null;
        }
    }


    @Override
    public Class<CarddavSettings> syncSettingsType() {
        return CarddavSettings.class;
    }

}
