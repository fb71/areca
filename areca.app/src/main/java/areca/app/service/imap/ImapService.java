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

import static org.apache.commons.lang3.StringUtils.abbreviate;

import java.util.List;

import org.apache.commons.lang3.mutable.MutableInt;

import org.polymap.model2.runtime.UnitOfWork;

import areca.app.ArecaApp;
import areca.app.service.Message2ContactAnchorSynchronizer;
import areca.app.service.Message2PseudoContactAnchorSynchronizer;
import areca.app.service.Service;
import areca.app.service.SyncableService;
import areca.app.service.imap.ImapRequest.LoginCommand;
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
        return "Messages - EMail";
    }


    @Override
    public Sync newSync( SyncContext ctx ) {
        return new Sync() {
            UnitOfWork uow = ctx.uowFactory.supply();
            Message2ContactAnchorSynchronizer messages2ContactAnchor = new Message2ContactAnchorSynchronizer( uow );
            Message2PseudoContactAnchorSynchronizer messages2PseudoAnchor = new Message2PseudoContactAnchorSynchronizer( uow );

            @Override
            public Promise<?> start() {
                ctx.monitor.beginTask( "EMail", ProgressMonitor.UNKNOWN );
                return fetchFolders()
                        // sync folders
                        .then( folderNames -> {
                            LOG.info( "Folders: %s", folderNames );
                            return Promise.joined( folderNames.size(), i -> syncFolder( folderNames.get( i ) ) );
                        })
                        .reduce2( 0, (result,folderCount) -> result + folderCount )
                        .map( total -> {
                            return uow.submit().onSuccess( submitted -> {
                                ctx.monitor.done();
                                LOG.info( "Submitted: %s, in ?? folders", total );
                            });
                        })
                        .onError( ArecaApp.instance().defaultErrorHandler() )
                        .onError( e -> ctx.monitor.done() );
            }


            protected Promise<Integer> syncFolder( String folderName ) {
                var subMonitor = ctx.monitor.subMonitor();
                return new ImapFolderSynchronizer( folderName, uow, () -> newRequest() )
                        .onMessageCount( msgCount -> subMonitor.beginTask( abbreviate( folderName, 5 ), msgCount*3 ) )

                        .start()
                        .onSuccess( msg -> {
                            //LOG.debug( "%s: pre sync: %s", folderName, msg.getClass() );
                            subMonitor.worked( 1 );
                        })

                        .thenOpt( msg -> messages2ContactAnchor.perform( msg.get() ) )
                        .onSuccess( msg -> subMonitor.worked( 1 ) )

                        .thenOpt( msg -> messages2PseudoAnchor.perform( msg.get() ) )
                        .onSuccess( msg -> subMonitor.worked( 1 ) )

                        .reduce( new MutableInt(), (r,msg) -> msg.ifPresent( m -> r.increment() ) )
                        .map( mutableInt -> {
                            LOG.info( "%s: synched messages: %s", folderName, mutableInt );
                            subMonitor.done();
                            return mutableInt.toInteger();
                        });
            }


            protected Promise<List<String>> fetchFolders() {
                var request = newRequest();
                request.commands.add( new FolderListCommand() );
                return request.submit()
                        .filter( FolderListCommand.class::isInstance )
                        .map( command -> ((FolderListCommand)command).folderNames );
            }
        };
    }


    protected ImapRequest newRequest() {
        return new ImapRequest( self -> {
            self.host = "mail.polymap.de";
            self.port = 993;
            self.loginCommand = new LoginCommand( "areca@polymap.de", "dienstag" );
        });
    }

}
