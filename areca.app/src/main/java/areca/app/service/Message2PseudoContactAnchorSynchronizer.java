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
package areca.app.service;

import static areca.app.service.Message2ContactAnchorSynchronizer.addressParts;

import java.util.HashMap;
import java.util.Map;

import org.polymap.model2.query.Expressions;
import org.polymap.model2.runtime.UnitOfWork;

import areca.app.model.Anchor;
import areca.app.model.Message;
import areca.common.Assert;
import areca.common.Platform;
import areca.common.ProgressMonitor;
import areca.common.Promise;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public class Message2PseudoContactAnchorSynchronizer {

    private static final Log LOG = LogFactory.getLog( Message2PseudoContactAnchorSynchronizer.class );

    private UnitOfWork              uow;

    private ProgressMonitor         monitor;

    private Map<String,Anchor>      seen = new HashMap<>( 512 );


    public Message2PseudoContactAnchorSynchronizer( UnitOfWork uow, ProgressMonitor monitor ) {
        this.uow = uow;
        this.monitor = monitor;
    }


    public Promise<Message> perform( Message message ) {
        // XXX all associated adresses
        var address = addressParts( message.from.get() );
        var storeRef = "pseudo-contact:" + address.pure;

        return uow.query( Anchor.class )
                // check/create Anchor
                .where( Expressions.eq( Anchor.TYPE.storeRef, storeRef ) )
                .executeToList()
                .map( rs -> {
                    if (!rs.isEmpty()) {
                        LOG.debug( "Pseudo contact found: %s", storeRef );
                        Assert.isEqual( 1, rs.size() );
                        return seen.computeIfAbsent( storeRef, __ -> rs.get( 0 ) );
                    }
                    return null;
                })
                .then( (Anchor pseudo) -> {
                    // Anchor exists
                    if (pseudo != null) {
                        pseudo.messages.add( message );
                        return Platform.async( () -> message );
                    }
                    // no Anchor yet
                    else {
                        return uow.query( Message.class )
                                .where( Expressions.eq( Message.TYPE.from, message.from.get() ) ) // XXX all addresses
                                .executeToList()
                                .map( foundMsgs -> {
                                    if (foundMsgs.size() > 1) {
                                        var anchor = seen.computeIfAbsent( storeRef, __ -> uow.createEntity( Anchor.class, proto -> {
                                            LOG.debug( "Pseudo contact create: %s -> '%s' '%s' '%s'", message.from.get(), address.first, address.last, address.pure );
                                            proto.name.set( String.format( "%s %s", address.first, address.last ) );
                                            proto.storeRef.set( storeRef );
                                        }));
                                        // FIXME should add all found, and check for found Anchors if the messsage is already there;
                                        // currently it works just for messages of the same run (!?)
                                        anchor.messages.add( message );
                                    }
                                    return message;
                                });
                    }
                });
    }

}
