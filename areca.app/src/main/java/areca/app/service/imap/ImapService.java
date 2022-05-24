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

import org.apache.commons.lang3.mutable.MutableInt;

import areca.app.ArecaApp;
import areca.app.service.Message2ContactAnchorSynchronizer;
import areca.app.service.Message2PseudoContactAnchorSynchronizer;
import areca.app.service.Service;
import areca.app.service.SyncableService;
import areca.app.service.imap.ImapRequest.LoginCommand;
import areca.common.Platform;
import areca.common.ProgressMonitor;
import areca.common.Promise;
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
            public Promise<?> start() {
                var uow = ArecaApp.instance().repo().newUnitOfWork();
                var messages2ContactAnchor = new Message2ContactAnchorSynchronizer( uow, monitor );
                var messages2PseudoAnchor = new Message2PseudoContactAnchorSynchronizer( uow, monitor );

                return new ImapFolderSynchronizer( "Test1", uow, () -> newRequest(), monitor)
                        .start()
                        .onSuccess( msg -> monitor.worked( 1 ) )

                        .then( msg -> messages2ContactAnchor.perform( msg ) )
                        .onSuccess( msg -> monitor.worked( 1 ) )

                        .then( msg -> messages2PseudoAnchor.perform( msg ) )
                        .onSuccess( msg -> monitor.worked( 1 ) )

                        .reduce( new MutableInt(), (r,msg) -> r.increment())
                        .map( count -> {
                            return uow.submit().onSuccess( submitted -> {
                                monitor.done();
                                LOG.info( "Submitted: %s", count );
                            });
                        })
                        .onError( ArecaApp.instance().defaultErrorHandler() )
                        .onError( e -> monitor.done() );

//                monitor.beginTask( "EMail", 10 );
//                pseudoWork();
            }

            private void pseudoWork() {
                monitor.worked( 1 );
                if ((work -= 1) > 0) {
                    Platform.schedule( 1000, () -> pseudoWork() );
                } else {
                    monitor.done();
                }
            }
        };
    }


    protected Promise<String> fetchFolders() {
        throw new RuntimeException( "not yet...");
    }


    protected ImapRequest newRequest() {
        return new ImapRequest( self -> {
            self.host = "mail.polymap.de";
            self.port = 993;
            self.loginCommand = new LoginCommand( "areca@polymap.de", "dienstag" );
        });
    }

}
