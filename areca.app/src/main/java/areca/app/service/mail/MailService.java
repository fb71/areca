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
package areca.app.service.mail;

import static org.apache.commons.lang3.StringUtils.abbreviate;
import static org.polymap.model2.runtime.EntityRuntimeContext.EntityStatus.REMOVED;
import static org.polymap.model2.runtime.Lifecycle.State.AFTER_SUBMIT;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableObject;

import org.polymap.model2.runtime.UnitOfWork;

import areca.app.ArecaApp;
import areca.app.model.Address;
import areca.app.model.EntityLifecycleEvent;
import areca.app.model.ImapSettings;
import areca.app.model.Message;
import areca.app.model.ModelUpdateEvent;
import areca.app.model.SmtpSettings;
import areca.app.service.ContactAnchorSynchronizer;
import areca.app.service.Service;
import areca.app.service.SyncableService;
import areca.app.service.TransportService;
import areca.app.service.mail.MailFolderSynchronizer.MessageStoreRef;
import areca.common.Assert;
import areca.common.ProgressMonitor;
import areca.common.Promise;
import areca.common.base.Sequence;
import areca.common.event.EventManager;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public class MailService
        extends Service
        implements SyncableService, TransportService {

    private static final Log LOG = LogFactory.getLog( MailService.class );


    public MailService() {
        onMessageUnreadSet();
        onMessageDeleted();
        onMessageSent();
    }


    @Override
    public String label() {
        return "Mail";
    }


    protected void onMessageDeleted() {
        EventManager.instance().subscribe( (EntityLifecycleEvent ev) -> {
            if (ev.state == AFTER_SUBMIT
                    && ev.getSource().status() == REMOVED
                    && ev.getSource() instanceof Message) {

                var msg = (Message)ev.getSource();
                LOG.info( "On REMOVED: message: %s", msg.storeRef );
                msg.storeRef( MessageStoreRef.class ).ifPresent( storeRef -> {
                    loadImapSettings().then( settings -> {
                        if (settings != null) {
                            return new MessageDeleteRequest( settings.toRequestParams(), storeRef.msgId() ).submit()
                                    .onSuccess( command -> LOG.info( "On REMOVED: deleted (%s)", command.count() ) );
                        }
                        else {
                            return Promise.completed( null );
                        }
                    })
                    .onError( ArecaApp.current().defaultErrorHandler() );
                });
            }
        })
        .performIf( EntityLifecycleEvent.class::isInstance );
    }


    /**
     * Model update: set SEEN flag
     */
    protected void onMessageUnreadSet() {
        EventManager.instance().subscribe( (ModelUpdateEvent ev) -> {
            var mySettings = new MutableObject<ImapSettings>( null );
            loadImapSettings()
                .then( settings -> {
                    mySettings.setValue( settings );
                    UnitOfWork uow = ArecaApp.current().unitOfWork();
                    return settings != null ? ev.entities( Message.class, uow ) : Promise.completed( null );
                })
                .then( entities -> {
                    if (entities == null || entities.isEmpty()) {
                        LOG.info( "On UNREAD: Message/Entity removed or no IMAP settings" );
                        return Promise.completed( null );
                    }
                    else {
                        var message = entities.get( 0 );
                        Assert.isEqual( false, message.unread.get() );
                        return message.storeRef( MessageStoreRef.class )
                                .map( storeRef -> {
                                    RequestParams params = mySettings.getValue().toRequestParams();
                                    return new MessageSetFlagRequest( params, storeRef.msgId() ).submit()
                                            .onSuccess( command -> LOG.info( "On UNREAD: SEEN flag set (%s)", command.count() ) );
                                })
                                .orElse( Promise.completed( null ) );
                    }
                })
                .onError( ArecaApp.current().defaultErrorHandler() );
        })
        // XXX horrible condition!
        .performIf( ModelUpdateEvent.class, ev -> ev.entities( Message.class ).size() == 1 );
    }


    protected void onMessageSent() {
//      // MessageSentEvent: append sent messages to Sent folder
//      EventManager.instance()
//              .subscribe( (MessageSentEvent ev) -> {
//                  LOG.info( "Message sent: attaching to Sent folder..." );
//                  loadImapSettings()
//                          .then( settings -> {
//                              if (settings != null) {
//                                  ImapRequest request = new ImapRequest( self -> {
//                                      self.host = settings.host.get();
//                                      self.port = settings.port.get();
//                                      self.loginCommand = new LoginCommand( settings.username.get(), settings.pwd.get() );
//                                  });
//                                  request.commands.addAll( new AppendCommand( "Sent", ev.sent.message, ev.sent.from ).commands );
//                                  return request.submit();
//                              }
//                              else {
//                                  return Promise.completed( null );
//                              }
//                          })
//                          .onSuccess( command -> LOG.info( "OK" ))
//                          .onError( ArecaApp.current().defaultErrorHandler() );
//
//              })
//              .performIf( MessageSentEvent.class::isInstance );  // FIXME sender was email :)
    }


    /**
     * Default: load from ArecaApp
     */
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
                case INCREMENTAL : return new IncrementalSync( settings, ctx );
                case BACKGROUND : return null;
                default: return null;
            }
        });
    }


    /**
     *
     */
    protected static class FullSync
            extends SyncBase {

        public FullSync( ImapSettings settings, SyncContext ctx ) {
            super( settings, ctx );
        }

        @Override
        protected List<String> folders( List<String> folderNames ) {
            folderNames.remove( "Trash" );
            folderNames.remove( "Junk" );
            folderNames.remove( "Drafts" );
            return folderNames;
        }

        @Override
        protected int monthsToSync() {
            return settings.monthsToSync.get();
        }
    }


    /**
     *
     */
    protected static class IncrementalSync
            extends SyncBase {

        public IncrementalSync( ImapSettings settings, SyncContext ctx ) {
            super( settings, ctx );
        }

        @Override
        protected List<String> folders( List<String> folderNames ) {
            return Arrays.asList( "INBOX", "Sent" );
        }

        @Override
        protected int monthsToSync() {
            return 1;  // XXX maybe to long, maybe to short! find proper solution
        }
    }


    /**
     *
     */
    protected static abstract class SyncBase
            extends Sync {

        protected SyncContext   ctx;

        protected UnitOfWork    uow;

        protected ContactAnchorSynchronizer contactAnchorSync;

        protected PseudoContactSynchronizer pseudoContactSync;

        protected ImapSettings  settings;


        public SyncBase( ImapSettings settings, SyncContext ctx ) {
            this.settings = settings;
            this.ctx = ctx;
            uow = ctx.unitOfWork();
            contactAnchorSync = new ContactAnchorSynchronizer( uow );
            pseudoContactSync = new PseudoContactSynchronizer( uow, settings );
        }


        protected abstract List<String> folders( List<String> folderNames );


        protected abstract int monthsToSync();


        @Override
        public Promise<?> start() {
            ctx.monitor().beginTask( "Mail", ProgressMonitor.UNKNOWN );
            return fetchFolders()
                    // sync + submit folders
                    .then( allFolderNames -> {
                        var folderNames = folders( allFolderNames );
                        LOG.info( "Folders: %s", folderNames );

                        ctx.monitor().setTotalWork( folderNames.size() * 100 );
                        return Promise.serial( folderNames.size(), i -> {
                            return syncFolder( folderNames.get( i ) )
                                    .then( __ -> uow.submit() )
                                    .onSuccess( __ -> LOG.info( "%s: submitted", folderNames.get( i ) ) );
                        });
                    })
                    .reduce2( 0, (result,submitted) -> result + 1 )
                    .onSuccess( totalFolders -> {
                        ctx.monitor().done();
                        LOG.info( "Done: %d folders", totalFolders );
                    });
        }


        protected Promise<Integer> syncFolder( String folderName ) {
            var subMonitor = ctx.monitor().subMonitor( 100 );
            return new MailFolderSynchronizer( folderName, uow, settings )
                    .onMessageCount( msgCount -> subMonitor.beginTask( abbreviate( folderName, 5 ), msgCount ) )
                    .start()
                    .onSuccess( msg -> {
                        LOG.debug( "%s: pre sync: %s", folderName, msg.getClass() );
                    })

                    .thenOpt( msg -> contactAnchorSync.perform( msg.get() ) )
                    .thenOpt( msg -> pseudoContactSync.perform( msg.get() ) )

                    .onSuccess( msg -> subMonitor.worked( 1 ) )
                    .reduce( new MutableInt(), (r,msg) -> msg.ifPresent( m -> r.increment() ) )
                    .map( mutableInt -> {
                        LOG.info( "%s: synched messages: %s", folderName, mutableInt );
                        subMonitor.done();
                        return mutableInt.toInteger();
                    });
        }


        protected Promise<List<String>> fetchFolders() {
            return new AccountInfoRequest( settings.toRequestParams() ).submit()
                    .map( accountInfo -> Sequence.of( accountInfo.folderNames() ).toList() );
        }
    }


    // TransportService ***********************************

    @Override
    public Promise<List<Transport>> newTransport( Address receipient, TransportContext ctx ) {
        var email = EmailAddress.check( receipient );
        if (email.isPresent()) {
            return ArecaApp.current().settings()
                    .then( uow -> uow.query( SmtpSettings.class ).executeCollect() )
                    .map( rs -> Sequence.of( rs )
                            .map( settings -> (Transport)new MailTransport( settings, email.get(), ctx ) )
                            .toList() );
        }
        else {
            return Promise.completed( Collections.emptyList() );
        }
    }


    /**
     *
     */
    protected class MailTransport
            extends Transport {

        protected SmtpSettings      settings;

        protected EmailAddress      receipient;

        protected TransportContext  ctx;

        public MailTransport( SmtpSettings settings, EmailAddress receipient, TransportContext ctx ) {
            this.settings = settings;
            this.receipient = receipient;
            this.ctx = ctx;
        }


        @Override
        public Promise<Sent> send( TransportMessage msg ) {
            var monitor = ctx.newMonitor().beginTask( "Send", 9 ).worked( 1 );
            throw new RuntimeException( "..." );
//            var request = new SmtpRequest( self -> {
//                self.host = settings.host.get();
//                self.port = settings.port.get();
//                LOG.info( "Hostname: ", ArecaApp.hostname() );
//                self.loginCommand = new HeloCommand( ArecaApp.hostname() );
//                self.commands.add( new AuthPlainCommand( settings.username.get(), settings.pwd.get() ) );
//                self.commands.add( new MailFromCommand( settings.from.get() ) );
//                self.commands.add( new RcptToCommand( receipient.content ) );
//                self.commands.add( new DataCommand() );
//                self.commands.add( new DataContentCommand( msg.threadSubject.orElse( "" ), msg.text ) );
//                self.commands.add( new QuitCommand() );
//            });
//            return request.submit()
//                    .onSuccess( command -> {
//                        LOG.info( "Response: %s", command );
//                        monitor.worked( 1 );
//                    })
//                    .onError( e -> {
//                        LOG.info( "Error: %s", e );
//                    })
//                    .reduce( new ArrayList<>(), (r,c) -> r.add( c ) )
//                    .map( l -> new Sent() {{
//                        message = msg;
//                        from = settings.from.get();
//                        monitor.done();
//                    }});
        }
    }

}
