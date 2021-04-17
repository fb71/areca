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
import areca.common.base.Consumer;
import areca.common.base.Opt;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Property;

/**
 *
 * @author Falko Bräutigam
 */
public class ImapFolderSynchronizer {

    private static final Log log = LogFactory.getLog( ImapFolderSynchronizer.class );

    public Property<Boolean>            checkExisting = Property.create( this, "checkExisting", false );

//    protected Supplier.$<ImapRequest>   requestFactory;

    protected EntityRepository          repo;

    public Opt<Exception>               exception = Opt.absent();


    public <E extends Exception> ImapFolderSynchronizer( Consumer<ImapFolderSynchronizer,E> initializer ) throws E {
        initializer.accept( this );
    }


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
