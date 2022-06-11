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
import areca.app.model.ImapSettings;
import areca.app.service.Message2ContactAnchorSynchronizer;
import areca.app.service.Message2PseudoContactAnchorSynchronizer;
import areca.app.service.MessageSentEvent;
import areca.app.service.Service;
import areca.app.service.SyncableService;
import areca.app.service.imap.ImapRequest.LoginCommand;
import areca.common.ProgressMonitor;
import areca.common.Promise;
import areca.common.event.EventManager;
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


    public ImapService() {
        // MessageSentEvent: append sent messages to Sent folder
        EventManager.instance()
                .subscribe( (MessageSentEvent ev) -> {
                    LOG.info( "Message sent: attaching to Sent folder..." );
                    loadImapSettings()
                            .then( settings -> {
                                if (settings != null) {
                                    ImapRequest request = new ImapRequest( self -> {
                                        self.host = settings.host.get();
                                        self.port = settings.port.get();
                                        self.loginCommand = new LoginCommand( settings.username.get(), settings.pwd.get() );
                                    });
                                    request.commands.addAll( new AppendCommand( "Sent", ev.sent.message, ev.sent.from ).commands );
                                    return request.submit();
                                }
                                else {
                                    return Promise.completed( null );
                                }
                            })
                            .onSuccess( command -> LOG.info( "OK" ))
                            .onError( ArecaApp.current().defaultErrorHandler() );

                })
                .performIf( MessageSentEvent.class::isInstance );  // FIXME sender was email :)
    }


    @Override
    public String label() {
        return "Email";
    }


    /** Default: load from ArecaApp */
    protected Promise<ImapSettings> loadImapSettings() {
        return ArecaApp.instance().settings().then( uow -> {
            return uow.query( ImapSettings.class )
                    .executeCollect()
                    .map( list -> !list.isEmpty() ? list.get( 0 ) : null );
        });
    }


    @Override
    public Promise<Sync> newSync( SyncType syncType, SyncContext ctx ) {
        return loadImapSettings().map( settings -> {
            if (settings == null) {
                return null;
            }
            switch (syncType) {
                case FULL : return new FullSync( settings, ctx );
                case INCREMENT : return null;
                case BACKGROUND : return null;
                default: return null;
            }
        });
    }


    /**
     *
     */
    protected static class FullSync
            extends Sync {

        protected ImapSettings  settings;

        protected SyncContext   ctx;

        protected UnitOfWork    uow;

        protected Message2ContactAnchorSynchronizer messages2ContactAnchor;

        protected Message2PseudoContactAnchorSynchronizer messages2PseudoAnchor;

        public FullSync( ImapSettings settings, SyncContext ctx ) {
            this.settings = settings;
            this.ctx = ctx;
            uow = ctx.uowFactory.supply();
            messages2ContactAnchor = new Message2ContactAnchorSynchronizer( uow );
            messages2PseudoAnchor = new Message2PseudoContactAnchorSynchronizer( uow );
        }


        protected ImapRequest newRequest() {
            return new ImapRequest( self -> {
                self.host = settings.host.get();
                self.port = settings.port.get();
                self.loginCommand = new LoginCommand( settings.username.get(), settings.pwd.get() );
            });
        }


        @Override
        public Promise<?> start() {
            ctx.monitor.beginTask( "EMail", ProgressMonitor.UNKNOWN );
            return fetchFolders()
                    // sync folders
                    .then( folderNames -> {
                        LOG.info( "Folders: %s", folderNames );
                        return Promise.serial( folderNames.size(), i -> syncFolder( folderNames.get( i ) ) );
                    })
                    .reduce2( 0, (result,folderCount) -> result + folderCount )
                    .map( total -> {
                        return uow.submit().onSuccess( submitted -> {
                            ctx.monitor.done();
                            LOG.info( "Submitted: %s, in ?? folders", total );
                        });
                    });
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
    }

}
