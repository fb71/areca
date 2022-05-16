/*
 * Copyright (C) 2020-2022, the @authors. All rights reserved.
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

import static areca.app.service.imap.MessageFetchHeadersCommand.FieldEnum.DATE;
import static areca.app.service.imap.MessageFetchHeadersCommand.FieldEnum.FROM;
import static areca.app.service.imap.MessageFetchHeadersCommand.FieldEnum.MESSAGE_ID;
import static areca.app.service.imap.MessageFetchHeadersCommand.FieldEnum.SUBJECT;
import static areca.app.service.imap.MessageFetchHeadersCommand.FieldEnum.TO;
import static org.apache.commons.lang3.Range.between;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableInt;

import org.polymap.model2.query.Expressions;
import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.UnitOfWork;

import areca.app.model.Message;
import areca.app.service.Messages2ContactAnchorSynchronizer;
import areca.app.service.imap.MessageFetchHeadersCommand.FieldEnum;
import areca.common.ProgressMonitor;
import areca.common.Promise;
import areca.common.base.Sequence;
import areca.common.base.Supplier.RSupplier;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * Synchronizes an entire IMAP folder with the DB.
 *
 * @author Falko Bräutigam
 */
public class ImapFolderSynchronizer {

    private static final Log LOG = LogFactory.getLog( ImapFolderSynchronizer.class );

//    public Property<Boolean>            checkExisting = Property.create( this, "checkExisting", false );

    protected ProgressMonitor           monitor;

    protected RSupplier<ImapRequest>    requestFactory;

    protected EntityRepository          repo;

    protected String                    folderName;


    public ImapFolderSynchronizer( String folderName, EntityRepository repo, RSupplier<ImapRequest> requestFactory, ProgressMonitor monitor ) {
        this.repo = repo;
        this.requestFactory = requestFactory;
        this.folderName = folderName;
        this.monitor = monitor;
    }


    public Promise<?> start() {
        monitor.beginTask( "EMail", ProgressMonitor.UNKNOWN );
        monitor.subTask( folderName );

        var uow = repo.newUnitOfWork();

        return fetchMessageCount()
                // fetch messages-ids
                .then( msgCount -> {
                    LOG.info( "Exists: " + msgCount );
                    monitor.beginTask( "EMail", (msgCount*2)+2 );
                    monitor.worked( 1 );
                    return fetchMessageIds( msgCount );
                })
                // find missing Message entities
                .then( (Map<String,Integer> msgIds) -> {
                    monitor.worked( 1 );

                    // query Messages that are in the store
                    String[] queryIds = msgIds.keySet().toArray( String[]::new );
                    return uow.query( Message.class )
                            .where( Expressions.eqAny( Message.TYPE.storeRef, queryIds ) )
                            .execute()
                            // reduce to not-found msgIds
                            .reduce( new HashMap<String,Integer>( msgIds ), (r,entity) -> {
                                entity.ifPresent( _entity -> {
                                    r.remove( _entity.storeRef.get() );
                                });
                            });
                })
                // MESSAGE_ID -> message num
                .onSuccess( (HashMap<String,Integer> missingMsgIds) -> {
                    LOG.info( "Not found: " + missingMsgIds );
                })
                // fetch IMAP messages
                .then( notFound -> {
                    if (notFound.isEmpty()) {
                        monitor.done();
                        return Promise.stop();
                    } else {
                        var msgNums = notFound.values().iterator();
                        return Promise.joined( notFound.size(), i -> fetchMessage( msgNums.next(), uow ) );
                    }
                })
                .onSuccess( msg -> {
                    monitor.worked( 1 );
                })
                .then( (Message msg) -> new Messages2ContactAnchorSynchronizer( uow, monitor ).perform( msg ) )
                // submit
                .reduce( new MutableInt(), (r,entity) -> {
                    r.increment();
                    monitor.worked( 1 );
                })
                .map( count -> {
                    return uow.submit().onSuccess( submitted -> {
                        monitor.done();
                        LOG.info( "Submitted: %s", count );
                    });
                });
    }


    protected Promise<Integer> fetchMessageCount() {
        var r = requestFactory.supply();
        r.commands.add( new FolderSelectCommand( folderName ) );
        return r.submit()
                .filter( command -> command instanceof FolderSelectCommand )
                .map( command -> ((FolderSelectCommand)command).exists );
    }


    /** MESSAGE_ID -> message num */
    protected Promise<Map<String,Integer>> fetchMessageIds( int msgNum ) {
        var r = requestFactory.supply();
        r.commands.add( new FolderSelectCommand( folderName ) );
        r.commands.add( new MessageFetchHeadersCommand( between( 1, msgNum ), MESSAGE_ID ) );
        return r.submit()
                .filter( command -> command instanceof MessageFetchHeadersCommand )
                .map( command -> {
                    return Sequence.of( ((MessageFetchHeadersCommand)command).headers.entrySet() )
                            .toMap( entry -> entry.getValue().get( MESSAGE_ID ), entry -> entry.getKey() );
                });
    }


    protected Promise<Message> fetchMessage( int msgNum, UnitOfWork uow ) {
        var r = requestFactory.supply();
        r.commands.add( new FolderSelectCommand( folderName ) );
        r.commands.add( new MessageFetchCommand( msgNum, "TEXT" ) );
        r.commands.add( new MessageFetchHeadersCommand( between( msgNum, msgNum ), SUBJECT, FROM, TO, DATE, MESSAGE_ID ) );
        return r.submit()
                .reduce( uow.createEntity( Message.class ), (entity,command) -> {
                    if (command instanceof MessageFetchCommand) {
                        entity.text.set( ((MessageFetchCommand)command).textContent );
                    }
                    else if (command instanceof MessageFetchHeadersCommand) {
                        Map<FieldEnum,String> headers = ((MessageFetchHeadersCommand)command).headers.get( msgNum );
                        entity.storeRef.set( headers.get( MESSAGE_ID ) );
                        entity.from.set( headers.get( FROM ) );
                    }
                });

    }

}
