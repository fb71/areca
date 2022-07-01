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

import org.polymap.model2.runtime.UnitOfWork;

import areca.app.model.Contact;
import areca.common.Assert;
import areca.common.NullProgressMonitor;
import areca.common.ProgressMonitor;
import areca.common.Promise;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.Property;
import areca.ui.component2.Property.ReadWrite;

/**
 *
 * @author Falko Bräutigam
 */
public class CarddavSynchronizer {

    private static final Log LOG = LogFactory.getLog( CarddavSynchronizer.class );

    public ReadWrite<?,ProgressMonitor> monitor = Property.rw( this, "monitor", new NullProgressMonitor() );

    protected static final Contact      END = new Contact();

    protected DavResource               contactsRoot;

    protected UnitOfWork                uow;



    public CarddavSynchronizer( DavResource contactsRoot, UnitOfWork uow ) {
        this.contactsRoot = contactsRoot;
        this.uow = uow;
    }


    public Promise<List<Contact>> start() {
        monitor.value().beginTask( "CardDav", ProgressMonitor.UNKNOWN );

        LOG.debug( "URL: %s", contactsRoot.url() );
        return new PropfindRequest( contactsRoot )
                // find all DavResource
                .submit()
                // get vcf content
                .then( res -> {
                    LOG.debug( "PROPFIND: %s", Arrays.asList( res ) );
                    monitor.value().setTotalWork( res.length );
                    return Promise.joined( res.length, i -> {
                        return new GetResourceRequest( res[i] ).submit();
                    });
                })
                // parse VCard -> query Contact
                .then( vcf -> {
                    var vcard = VCard.parse( vcf.text() );
                    LOG.info( "VCard fetched: %s", vcard.fn.value() );
                    return uow.query( Contact.class )
                            .where( eq( Contact.TYPE.storeRef, vcard.uid.value() ) )
                            .executeCollect()
                            .map( contacts -> Pair.of( vcard, contacts ) );
                })
                // create/update Contact
                .map( compound -> {
                    List<Contact> contacts = compound.getRight();
                    LOG.info( "Contacts found for '%s': %s", compound.getLeft().fn.value(), compound.getRight() );
                    Assert.that( contacts.size() <= 1 );
                    var contact = contacts.isEmpty()
                            ? uow.createEntity( Contact.class)
                            : contacts.get( 0 );
                    fillContact( contact, compound.getLeft() );
                    LOG.info( "Contact: %s", contact );
                    monitor.value().worked( 1 );
                    return contact;
                })
                .reduce( new ArrayList<Contact>(), (r,c) -> r.add( c ) )
                .then( contacts -> {
                    return uow.submit().map( submitted -> {
                        monitor.value().done();
                        return contacts;
                    });
                });
    }


    protected void fillContact( Contact contact, VCard vcard ) {
        vcard.firstname.opt().ifPresent( v -> contact.firstname.set( v ) );
        vcard.lastname.opt().ifPresent( v -> contact.lastname.set( v ) );
        vcard.uid.opt().ifPresent( v -> contact.storeRef.set( v ) );
        vcard.emails.values().first().ifPresent( v -> contact.email.set( v ) );  // XXX multiple
        vcard.phones.values().first().ifPresent( v -> contact.phone.set( v ) );  // XXX multiple
        vcard.photo.opt().ifPresent( v -> contact.photo.set( v ) );
    }

}
