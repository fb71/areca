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

import org.polymap.model2.query.Expressions;
import org.polymap.model2.runtime.UnitOfWork;

import areca.app.model.Address;
import areca.app.model.Anchor;
import areca.app.model.ImapSettings;
import areca.app.model.Message;
import areca.common.Promise;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * Creates {@link Anchor} for email message senders without a Contact.
 *
 * @author Falko Bräutigam
 */
public class PseudoContactSynchronizer {

    private static final Log LOG = LogFactory.getLog( PseudoContactSynchronizer.class );

    private UnitOfWork              uow;

    private ImapSettings            settings;


    public PseudoContactSynchronizer( UnitOfWork uow, ImapSettings settings ) {
        this.uow = uow;
        this.settings = settings;
    }


    public Promise<Message> perform( Message message ) {
        if (!message.fromAddress.opt().isPresent() || !message.toAddress.opt().isPresent()) {
            return Promise.completed( message, uow.priority() );
        }
        // XXX all associated adresses
        var address = Address.parseEncoded( message.outgoing.get()
                ? message.toAddress.get()
                : message.fromAddress.get() );

        var storeRef = new AnchorStoreRef( settings, address );

        return uow.ensureEntity( Anchor.class,
                Expressions.eq( Anchor.TYPE.storeRef, storeRef.encoded() ),
                proto -> {
                    LOG.debug( "Pseudo contact: %s", address.toString() );
                    proto.name.set( address.content.replace( "@", "@ " ) );
                    proto.setStoreRef( storeRef );
                })
                .map( anchor -> {
                    anchor.messages.add( message );
                    return message;
                });
    }


    /**
     *
     */
    public static class AnchorStoreRef
            extends MailStoreRef {

        /** Decode */
        public AnchorStoreRef() { }

        public AnchorStoreRef( ImapSettings settings, Address address ) {
            super( settings );
            parts.add( address.content );
        }

        @Override
        public String prefix() {
            return super.prefix() + "pseudo";
        }

        public String address() {
            return parts.get( 1 );
        }
    }

}
