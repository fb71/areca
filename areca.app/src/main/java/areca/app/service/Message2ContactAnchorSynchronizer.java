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

import areca.app.model.Anchor;
import areca.app.model.Contact;
import areca.app.model.Message;
import areca.common.Platform;
import areca.common.ProgressMonitor;
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

    private ProgressMonitor         monitor;

    private HashMap<String,Contact> seen = new HashMap<>( 512 );


    public Message2ContactAnchorSynchronizer( UnitOfWork uow, ProgressMonitor monitor ) {
        this.uow = uow;
        this.monitor = monitor;
    }


    public Promise<Message> perform( Message message ) {
        var address = addressParts( message.from.get() );

        return uow.query( Contact.class )
                // query Contact
                .where( Expressions.eq( Contact.TYPE.email, address.pure ) )
                .executeToList()
                .map( results -> {
                    if (!results.isEmpty()) {
                        LOG.debug( "Contact found for: %s", address.pure );
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


    public static Pattern       EMAIL_EXT = Pattern.compile( "([^<]+)[ ]*<([^>]+)>" );

    public static Pattern       NAME_COMMA = Pattern.compile( "([^,]+),[ ]*(.+)" );

    public static Pattern       NAME_SIMPLE = Pattern.compile( "([^ ]+)[ ]*(.*)" );

    public static Pattern       NAME_DOT = Pattern.compile( "([^.]+)[.](.+)" );

    public static Pattern       EMAIL = Pattern.compile( "([^@]+)@(.*)" );


    public static Address addressParts( String email ) {
        // extended
        var extMatch = EMAIL_EXT.matcher( email );
        if (extMatch.matches()) {
            String name = extMatch.group( 1 ).replace( "\"", "" ).trim();
            // comma separated name
            var commaMatch = NAME_COMMA.matcher( name );
            if (commaMatch.matches()) {
                return new Address() {{first = commaMatch.group( 2 ); last = commaMatch.group( 1 ); pure = extMatch.group( 2 );}};
            }
            // normal name
            var normalMatch = NAME_SIMPLE.matcher( name );
            if (normalMatch.matches()) {
                return new Address() {{first = normalMatch.group( 1 ); last = normalMatch.group( 2 ); pure = extMatch.group( 2 );}};
            }
            throw new RuntimeException( "No name match: " + name );
        }
        // just an email address
        var emailMatch = EMAIL.matcher( email );
        if (emailMatch.matches()) {
            var name = emailMatch.group( 1 );
            var dorMatch = NAME_DOT.matcher( name );
            if (dorMatch.matches()) {
                return new Address() {{first = dorMatch.group( 1 ); last = dorMatch.group( 2 ); pure = email;}};
            }
            else {
                return new Address() {{first = name; last = ""; pure = email;}};
            }
        }
        throw new RuntimeException( "No email match: " + email );
    }


    public static class Address {
        String first;
        String last;
        String pure;
    }


    protected static Opt<Matcher> match( String s, Pattern p ) {
        var matcher = p.matcher( s );
        return Opt.of( matcher.matches() ? matcher : null );
    }
}
