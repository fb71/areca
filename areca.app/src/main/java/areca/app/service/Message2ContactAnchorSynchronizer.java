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

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;
import org.polymap.model2.query.Expressions;
import org.polymap.model2.runtime.UnitOfWork;

import areca.app.model.Address;
import areca.app.model.Anchor;
import areca.app.model.Contact;
import areca.app.model.Message;
import areca.common.Assert;
import areca.common.Platform;
import areca.common.Promise;
import areca.common.base.Opt;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public class Message2ContactAnchorSynchronizer {

    private static final Log LOG = LogFactory.getLog( Message2ContactAnchorSynchronizer.class );

    private UnitOfWork              uow;

    private HashMap<String,Contact> seen = new HashMap<>( 512 );


    public Message2ContactAnchorSynchronizer( UnitOfWork uow ) {
        this.uow = uow;
    }


    public Promise<Message> perform( Message message ) {
        Assert.notNull( message );
        if (!message.fromAddress.opt().isPresent()) {
            return Promise.completed( message );
        }
        var address = Address.parseEncoded( message.fromAddress.get() );
        return uow.query( Contact.class )
                // query Contact
                .where( Expressions.eq( Contact.TYPE.email, address.content ) )
                .executeCollect()
                .map( results -> {
                    if (!results.isEmpty()) {
                        LOG.debug( "Contact found for: %s", address.content );
                        return results.get( 0 );
                        //return seen.computeIfAbsent( address.pure, __ -> results.get( 0 ) );
                    }
                    return null;
//                        return seen.computeIfAbsent( address.pure, __ -> uow.createEntity( Contact.class, proto -> {
//                            LOG.debug( "Contact create: %s -> '%s' '%s' '%s'", message.from.get(), address.first, address.last, address.pure );
//                            proto.firstname.set( address.first );
//                            proto.lastname.set( address.last );
//                            proto.email.set( address.pure );
//                        }));
//                    }
                })
                // fetch anchor
                .then( (Contact contact) -> {
                    return contact != null
                            ? contact.anchor.fetch().map( anchor -> Pair.of( contact, anchor ) )
                            : Platform.async( () -> Pair.of( (Contact)null, (Anchor)null ) );
                })
                // create anchor + attach message
                .map( (Pair<Contact,Anchor> contactAnchor) -> {
                    var contact = contactAnchor.getLeft();
                    if (contact != null) {
                        var anchor = contactAnchor.getRight();
                        if (anchor == null) {
                            anchor = uow.createEntity( Anchor.class, proto -> {
                                proto.name.set( anchorName( message, contactAnchor.getLeft() ) );
                                proto.storeRef.set( "contact:" + contactAnchor.getLeft().id() );
                            });
                            contact.anchor.set( anchor );
                        }
                        anchor.messages.add( message );
                    }
                    return message;
                });
    }


    protected String anchorName( Message msg, Contact contact ) {
        return contact.label();
    }


    protected static Opt<Matcher> match( String s, Pattern p ) {
        var matcher = p.matcher( s );
        return Opt.of( matcher.matches() ? matcher : null );
    }
}
