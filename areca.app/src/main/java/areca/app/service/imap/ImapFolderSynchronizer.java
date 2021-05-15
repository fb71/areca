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

import org.polymap.model2.runtime.EntityRepository;

import areca.app.model.Message;
import areca.common.NullProgressMonitor;
import areca.common.ProgressMonitor;
import areca.common.Promise;
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


    public Promise<Message> start() {
        monitor.get().beginTask( "Syncing folder: " + folderName, ProgressMonitor.UNKNOWN );

        var uow = repo.newUnitOfWork();

        return fetchMessageCount()
                // fetch messages
                .then( fsc -> {
                    monitor.get().beginTask( "Syncing folder: " + folderName, fsc.exists );
                    return Promise.joined( fsc.exists-1, i -> fetchMessage( i + 1 ) );
                })
                .map( fetched -> {
                    LOG.info( "Message: " + fetched.number );
                    return null;
                })
//                // load entity
//                .then( fetched -> {
//                    return uow.query( Message.class )
//                            //.where( )
//                            .execute()
//                            .map( entity -> new CallContext().put( entity ).put( fetched ) );
//                })
//                // check/create entity
//                .then( ctx -> {
//                    ctx.opt( Message.class ).ifPresentMap( entity -> new Promise.Completable<Message>() );
//                    return createMessage( fetched ).map( entity -> ctx.put( entity ) );
//                })
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


    protected Promise<MessageFetchCommand> fetchMessage( int msgNum ) {
        var r = requestFactory.supply();
        r.commands.add( new FolderSelectCommand( folderName ) );
        r.commands.add( new MessageFetchCommand( msgNum, "TEXT" ) );
        return r.submit()
                .filter( command -> command instanceof MessageFetchCommand )
                .map( command -> (MessageFetchCommand)command );
    }


//    protected Promise<Message> createMessage( MessageFetchCommand fetched ) {
//
//    }



//    public void process( String folderName, ProgressMonitor monitor ) {
//        var request = requestFactory.supply();
//        request.commands.add( )
//
//        try (
//            UnitOfWork uow = repo.newUnitOfWork();
//        ){
//            uow.createEntity( Message.class, null, (Message proto) -> {
//                SimpleDocument doc = SimpleDocument.parseXml( envelope.getValue() );
//                for (Element elm : doc.getElementsByTagName( "headers" )) {
//                    String[] kv = StringUtils.splitByWholeSeparator( elm.getFirstChild().getNodeValue(), "::" );
//                    // log.info( "KeyValue: " + Arrays.asList( kv ) );
//                    if (kv[0].equalsIgnoreCase( "from" )) {
//                        proto.from.set( kv[1] );
//                    }
//                }
//                doc.elementsByTagName( "htmlBody" ).first().ifPresent( elm -> {
//                    proto.text.set( elm.getFirstChild().getNodeValue() );
//                } );
//                doc.elementsByTagName( "plainBody" ).first().ifPresent( elm -> {
//                    proto.text.set( elm.getFirstChild().getNodeValue() );
//                } );
//                return proto;
//            } );
//            uow.commit();
//        }
//    }

}
