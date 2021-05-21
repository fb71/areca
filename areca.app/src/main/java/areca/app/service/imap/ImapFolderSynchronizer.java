/*
 * Copyright (C) 2020, the @authors. All rights reserved.
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

import static areca.app.service.imap.MessageFetchHeadersCommand.FieldEnum.MESSAGE_ID;
import static org.apache.commons.lang3.Range.between;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableInt;

import org.polymap.model2.query.Expressions;
import org.polymap.model2.runtime.EntityRepository;

import areca.app.model.Message;
import areca.common.NullProgressMonitor;
import areca.common.ProgressMonitor;
import areca.common.Promise;
import areca.common.base.Sequence;
import areca.common.base.Supplier.RSupplier;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Property;
import areca.ui.Property.ReadWrite;

/**
 * Synchronize an entire IMAP folder with the DB.
 *
 * @author Falko Bräutigam
 */
public class ImapFolderSynchronizer {

    private static final Log LOG = LogFactory.getLog( ImapFolderSynchronizer.class );

//    public Property<Boolean>            checkExisting = Property.create( this, "checkExisting", false );

    public ReadWrite<?,ProgressMonitor> monitor = Property.create( this, "monitor", new NullProgressMonitor() );

    protected RSupplier<ImapRequest>    requestFactory;

    protected EntityRepository          repo;

    protected String                    folderName;


    public ImapFolderSynchronizer( String folderName, EntityRepository repo, RSupplier<ImapRequest> requestFactory ) {
        this.repo = repo;
        this.requestFactory = requestFactory;
        this.folderName = folderName;
    }


    public Promise<?> start() {
        monitor.get().beginTask( "Syncing folder: " + folderName, ProgressMonitor.UNKNOWN );

        var uow = repo.newUnitOfWork();

        return fetchMessageCount()
                // fetch messages-ids
                .then( fsc -> {
                    monitor.get().beginTask( "Syncing folder: " + folderName, fsc.exists );
                    return fetchMessageIds( fsc.exists );
                })
                // find missing Message entities
                .then( fetched -> {
                    Map<String,Integer> msgIds = Sequence.of( fetched.headers.entrySet() )
                            .toMap( entry -> entry.getValue().get( MESSAGE_ID ), entry -> entry.getKey() );

                    // query Messages that are already there
                    String[] queryIds = msgIds.keySet().toArray( String[]::new );
                    return uow.query( Message.class )
                            .where( Expressions.eqAny( Expressions.template( Message.class, repo ).storeRef, queryIds ) )
                            .execute()
                            // reduce to not-found msgIds
                            .reduce( new HashMap<String,Integer>( msgIds ), (r,entity) -> {
                                entity.ifPresent( _entity -> {
                                    r.remove( _entity.storeRef.get() );
                                });
                            });
                })
                .onSuccess( missingMsgIds -> {
                    LOG.info( "Not-found: " + missingMsgIds );
                })
                // fetch IMAP messages
                .then( notFound -> {
                    var msgNums = notFound.values().iterator();
                    return Promise.joined( notFound.size(), i -> fetchMessage( msgNums.next() ) );
                })
                // create Message entities
                .map( mfc -> {
                    return uow.createEntity( Message.class, proto -> {
                        proto.storeRef.set( "..." );
                        proto.text.set( mfc.textContent );
                    });
                })
                // submit
                .reduce( new MutableInt(), (r,entity) -> r.increment() )
                .map( count -> {
                    return uow.submit().onSuccess( submitted -> {
                        LOG.info( "Submitted: %s / %s", count, submitted );
                    });
                })

//                .map( ctx -> {
//                    monitor.worked( 1 );
//                    return ctx.value( Message.class );
//                });
                ;
    }


    protected Promise<FolderSelectCommand> fetchMessageCount() {
        var r = requestFactory.supply();
        r.commands.add( new FolderSelectCommand( folderName ) );
        return r.submit()
                .filter( command -> command instanceof FolderSelectCommand )
                .map( command -> (FolderSelectCommand)command );
    }


    protected Promise<MessageFetchHeadersCommand> fetchMessageIds( int msgNum ) {
        var r = requestFactory.supply();
        r.commands.add( new FolderSelectCommand( folderName ) );
        r.commands.add( new MessageFetchHeadersCommand( between( 1, msgNum ), MESSAGE_ID ) );
        return r.submit()
                .filter( command -> command instanceof MessageFetchHeadersCommand )
                .map( command -> (MessageFetchHeadersCommand)command );
    }


    protected Promise<MessageFetchCommand> fetchMessage( int msgNum ) {
        var r = requestFactory.supply();
        r.commands.add( new FolderSelectCommand( folderName ) );
        r.commands.add( new MessageFetchCommand( msgNum, "TEXT" ) );
        return r.submit()
                .filter( command -> command instanceof MessageFetchCommand )
                .map( command -> (MessageFetchCommand)command );
    }


//    protected Anchor checkMessageContactAnchor( Message m ) {
//
//    }

}
