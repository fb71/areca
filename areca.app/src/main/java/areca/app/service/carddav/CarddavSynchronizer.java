/*
 * Copyright (C) 2021, the @authors. All rights reserved.
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
package areca.app.service.carddav;

import static org.polymap.model2.query.Expressions.eq;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import org.polymap.model2.runtime.EntityRepository;

import areca.app.model.Contact;
import areca.common.Assert;
import areca.common.NullProgressMonitor;
import areca.common.ProgressMonitor;
import areca.common.Promise;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Property;
import areca.ui.Property.ReadWrite;

/**
 *
 * @author Falko Bräutigam
 */
public class CarddavSynchronizer {

    private static final Log LOG = LogFactory.getLog( CarddavSynchronizer.class );

    public ReadWrite<?,ProgressMonitor> monitor = Property.create( this, "monitor", new NullProgressMonitor() );

    protected static final Contact      END = new Contact();

    protected DavResource               contactsRoot;

    protected EntityRepository          repo;



    public CarddavSynchronizer( DavResource contactsRoot, EntityRepository repo ) {
        this.contactsRoot = contactsRoot;
        this.repo = repo;
    }


    public Promise<?> start() {
        monitor.get().beginTask( "Syncing contacts", ProgressMonitor.UNKNOWN );

        var uow = repo.newUnitOfWork();
        return new PropfindRequest( contactsRoot )
                // find all DavResource
                .submit()
                // get vcf content
                .then( res -> {
                    LOG.info( "res: %s", Arrays.asList( res ) );
                    return Promise.joined( res.length, i -> {
                        return new GetResourceRequest( res[i] ).submit();
                    });
                })
                // parse VCard -> query Contact
                .then( vcf -> {
                    var vcard = VCard.parse( vcf.text() );
                    LOG.info( "VCard: %s", vcard.fn.get() );
                    return uow.query( Contact.class )
                            .where( eq( Contact.TYPE.storeRef, vcard.uid.get() ) )
                            .executeToList()
                            .map( contacts -> Pair.of( vcard, contacts ) );
                })
                // create/update Contact
                .map( compound -> {
                    List<Contact> contacts = compound.getRight();
                    LOG.info( "Contacts found for '%s': %s", compound.getLeft().fn.get(), compound.getRight() );
                    Assert.that( contacts.size() <= 1 );
                    var contact = contacts.isEmpty()
                            ? uow.createEntity( Contact.class)
                            : contacts.get( 0 );
                    fillContact( contact, compound.getLeft() );
                    LOG.info( "Contact: %s", contact );
                    return contact;
                })
                .reduce( new ArrayList<>(), (r,c) -> r.add( c ) )
                .then( contacts -> {
                    return uow.submit();
                });
    }


    protected void fillContact( Contact contact, VCard vcard ) {
        vcard.firstname.opt().ifPresent( v -> contact.firstname.set( v ) );
        vcard.lastname.opt().ifPresent( v -> contact.lastname.set( v ) );
        vcard.uid.opt().ifPresent( v -> contact.storeRef.set( v ) );
        vcard.emails.sequence().first().ifPresent( v -> contact.email.set( v ) );
        vcard.photo.opt().ifPresent( v -> contact.photo.set( v ) );
    }

}
