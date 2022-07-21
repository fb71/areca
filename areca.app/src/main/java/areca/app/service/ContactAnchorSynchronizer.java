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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.polymap.model2.query.Expressions;
import org.polymap.model2.runtime.UnitOfWork;

import areca.app.model.Address;
import areca.app.model.Anchor;
import areca.app.model.Contact;
import areca.app.model.Message;
import areca.app.model.StoreRef;
import areca.common.Assert;
import areca.common.Promise;
import areca.common.base.Opt;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * Checks if a given {@link Message} belongs to a {@link Contact} and its associated
 * {@link Anchor}.
 *
 * @author Falko Bräutigam
 */
public class ContactAnchorSynchronizer {

    private static final Log LOG = LogFactory.getLog( ContactAnchorSynchronizer.class );

    private UnitOfWork      uow;


    public ContactAnchorSynchronizer( UnitOfWork uow ) {
        this.uow = uow;
    }


    public Promise<Message> perform( Message message ) {
        Assert.notNull( message );
        if (!message.fromAddress.opt().isPresent() || !message.toAddress.opt().isPresent()) {
            return Promise.completed( message );
        }

        var address = Address.parseEncoded( message.outgoing.get()
                ? message.toAddress.get()
                : message.fromAddress.get() );

        return uow.query( Contact.class )
                .where( Expressions.eq( Contact.TYPE.email, address.content ) )
                .executeCollect()
                .map( rs -> {
                    if (rs.isEmpty()) {
                        return Promise.completed( message );
                    }
                    return Promise.serial( rs.size(), i -> {
                        var contact = rs.get( i );
                        LOG.debug( "Contact found for: %s", address.content );
                        return contact.anchor
                                .ensure( proto -> {
                                    proto.name.set( anchorName( message, contact ) );
                                    proto.setStoreRef( new ContactAnchorStoreRef( contact ) );
                                    proto.image.set( contact.photo.get() );
                                })
                                .map( anchor -> {
                                    anchor.messages.add( message );
                                    return anchor;
                                });
                    });
                })
                .reduce( message, (result,anchor) -> {} );
    }


    protected String anchorName( Message msg, Contact contact ) {
        return contact.label();
    }


    protected static Opt<Matcher> match( String s, Pattern p ) {
        var matcher = p.matcher( s );
        return Opt.of( matcher.matches() ? matcher : null );
    }


    /**
     *
     */
    public static class ContactAnchorStoreRef extends StoreRef {

        /** Decode */
        public ContactAnchorStoreRef() { }

        public ContactAnchorStoreRef( Contact contact ) {
            parts.add( contact.id().toString() );
        }

        public String contactId() {
            return parts.get( 0 );
        }

        @Override
        public String prefix() {
            return "contact";
        }
    }

}
