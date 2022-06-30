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

import org.polymap.model2.query.Expressions;
import org.polymap.model2.runtime.UnitOfWork;

import areca.app.model.Address;
import areca.app.model.Anchor;
import areca.app.model.Message;
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


    public Message2PseudoContactAnchorSynchronizer( UnitOfWork uow ) {
        this.uow = uow;
    }


    public Promise<Message> perform( Message message ) {
        if (!message.fromAddress.opt().isPresent() || !message.toAddress.opt().isPresent()) {
            return Promise.completed( message );
        }
        // XXX all associated adresses
        var address = Address.parseEncoded( message.outgoing.get()
                ? message.toAddress.get()
                : message.fromAddress.get() );

        var storeRef = "pseudo-contact:" + address.content;

        return uow.ensureEntity( Anchor.class,
                Expressions.eq( Anchor.TYPE.storeRef, storeRef ),
                proto -> {
                    LOG.debug( "Pseudo contact: %s", address.toString() );
                    proto.name.set( address.content.replace( "@", "@ " ) );
                    proto.storeRef.set( storeRef );
                })
                .map( anchor -> {
                    anchor.messages.add( message );
                    return message;
                });
    }

}
