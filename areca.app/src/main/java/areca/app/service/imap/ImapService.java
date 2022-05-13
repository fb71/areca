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
package areca.app.service.imap;

import areca.app.service.Service;
import areca.app.service.SyncableService;
import areca.common.Platform;
import areca.common.ProgressMonitor;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public class ImapService
        extends Service
        implements SyncableService {

    private static final Log LOG = LogFactory.getLog( ImapService.class );

    @Override
    public String label() {
        return "Messages - IMAP: ???";
    }

    @Override
    public Sync newSync( ProgressMonitor monitor ) {
        return new Sync() {
            int work = 10;

            @Override
            public void start() {
                monitor.beginTask( "EMail", 10 );
                work();
            }

            protected void work() {
                monitor.worked( 1 );
                if ((work -= 1) > 0) {
                    Platform.schedule( 1000, () -> work() );
                }
                else {
                    monitor.done();
                }
            }
        };
    }

}
