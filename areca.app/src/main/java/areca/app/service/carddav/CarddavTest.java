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

import static java.util.Arrays.asList;

import java.util.ArrayList;

import java.io.IOException;

import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.store.tidbstore.IDBStore;

import areca.app.model.Anchor;
import areca.app.model.Contact;
import areca.app.model.Message;
import areca.common.Assert;
import areca.common.MutableInt;
import areca.common.Platform;
import areca.common.Platform.HttpResponse;
import areca.common.Promise;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.common.testrunner.Skip;
import areca.common.testrunner.Test;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class CarddavTest {

    private static final Log LOG = LogFactory.getLog( CarddavTest.class );

    public static final ClassInfo<CarddavTest> info = CarddavTestClassInfo.instance();

    private static final String ARECA_CONTACTS_BASE = "https://polymap.de:8443";
    private static final String ARECA_CONTACTS_RES = "/dav/areca@polymap.de/Contacts/";
    private static final String ARECA_CONTACTS = "http?uri=https://polymap.de:8443/dav/areca@polymap.de/Contacts/";
    public static final String ARECA_USERNAME = "areca@polymap.de";
    public static final String ARECA_PWD = "dienstag";
    public static final DavResource ARECA_CONTACTS_ROOT = DavResource.create( ARECA_CONTACTS_BASE, ARECA_CONTACTS_RES ).auth( ARECA_USERNAME, ARECA_PWD );

    protected Promise<EntityRepository> initRepo( String name ) {
        return EntityRepository.newConfiguration()
                .entities.set( asList( Message.info, Contact.info, Anchor.info) )
                .store.set( new IDBStore( "CarddavTest-" + name, IDBStore.nextDbVersion(), true ) )
                .create();
    }


    @Test
    public Promise<HttpResponse> xhrTest() {
        return Platform.xhr( "GET", ARECA_CONTACTS )
                .authenticate( ARECA_USERNAME, ARECA_PWD )
                .onReadyStateChange( state -> LOG.info( "ReadyState: " + state ) )
                .submit()
                .onSuccess( response -> {
                    LOG.info( "Status: %s", response.status() );
                    Assert.that( response.status() < 299, "Wrong status: " + response.status() );
                    Assert.that( response.text().length() > 0 );
                    // System.out.println( response.text() );
                });
    }


    @Test
    public Promise<HttpResponse> propfindTest() {
        return Platform.xhr( "PROPFIND", ARECA_CONTACTS )
                .authenticate( ARECA_USERNAME, ARECA_PWD )
                .addHeader( "Content-Type", "application/xml" ) // ; charset=utf-8
                .addHeader( "Depth", "1" )
                .submit()
                .onSuccess( response -> {
                    LOG.info( "Status: %s", response.status() );
                    Assert.that( response.status() < 299, "Wrong status: " + response.status() );
                    Assert.that( response.text().length() > 0 );
                    //System.out.println( response.text() );
                });
    }


    @Test
    @Skip
    public Promise<?> spreadPromiseTest() {
        MutableInt count = new MutableInt( 0 );
        return Platform.async( () -> {
                    return 2;
                })
                .then( num -> {
                    LOG.info( "spread: " + num );
                    return Promise.joined( num, i -> Platform.async( () -> i ) );
                })
                .then( num -> {
                    LOG.info( "num2: " + num );
                    return Platform.async( () -> num );
//                    return Promise
//                            .joined( 2, i -> Platform.async( () -> num ) );
//                            //.reduce( new ArrayList<Integer>(), (r,n) -> r.add( n ) );
                })
                .onSuccess( i -> {
                    LOG.info( "Result: " + i );
                    Assert.that( count.getAndIncrement() < 2 );
                });
    }


    @Test
    public Promise<VCard> propfindVcfTest() {
        return new PropfindRequest( ARECA_CONTACTS_ROOT )
                .submit()
                .then( res -> {
                    LOG.info( "res: %s", res.length );
                    return Promise.joined( res.length, i -> new GetResourceRequest( res[i] ).submit() );
                })
                .map( vcf -> {
                    LOG.info( "VCF: %s", vcf.text() );
                    var vcard = VCard.parse( vcf.text() );
                    LOG.info( "VCard: %s", vcard );
                    return vcard;
                });
    }


    @Test
    public void vcardGroupTest() throws IOException {
        var vcf = "BEGIN:VCARD\n"
                + "VERSION:3.0\n"
                + "FN:Familiengruppe\n"
                + "N:Familiengruppe;;;;\n"
                + "REV:2018-12-05T22:51:31Z\n"
                + "UID:2453cbff-b7e7-4eb0-b2ad-b626f648e202\n"
                + "X-ADDRESSBOOKSERVER-MEMBER:urn:uuid:dc37e80a-2ebc-4e33-8fc2-716ac7c08c0d\n"
                + "\n"
                + " \n"
                + "X-ADDRESSBOOKSERVER-MEMBER:urn:uuid:82ce5742-f0ed-4692-81ce-c6326dff5f69\n"
                + "\n"
                + " \n"
                + "X-ADDRESSBOOKSERVER-KIND:group\n"
                + "END:VCARD";
        VCard.parse( vcf );
    }


    UnitOfWork uow = null;

    @Test
    @Skip
    public Promise<?> createContactsTest() {
        return initRepo( "createContacts" )
                .then( repo -> {
                    uow = repo.newUnitOfWork();
                    return Promise.joined( 2, i -> Platform.async( () -> {
                        return uow.createEntity( Contact.class, proto -> {
                            proto.firstname.set( "f" + i );
                            proto.lastname.set( "" );
                            proto.storeRef.set( "irgendwas" + i );
                        });
                        // return uow.submit();
                    }));
                })
                .reduce( new ArrayList<>(), (r,c) -> r.add( c ) )
                .then( created -> {
                    return uow.submit();
                });
    }


    @Test
    @Skip
    public Promise<?> synContactsTest() {
        return initRepo( "synContacts" ).then( repo -> {
            return new CarddavSynchronizer( ARECA_CONTACTS_ROOT, repo.newUnitOfWork() ).start();
        });
    }



//    @Test
//    @Skip
//    public Promise<HttpResponse> vcfTest() {
//        return Platform.xhr( "GET", "http?uri=https://polymap.de:8443/dav/areca@polymap.de/Contacts/c04ad627-c62a-408e-b4f4-f15c30bfd48b:349.vcf" )
//                .authenticate( ARECA_POLYMAP_USERNAME, ARECA_POLYMAP_PWD )
//                .submit()
//                .onSuccess( response -> {
//                    LOG.info( "Status: %s", response.status() );
//                    Assert.that( response.status() < 299, "Wrong status: " + response.status() );
//                    Assert.that( response.text().length() > 0 );
//                    //System.out.println( response.text() );
//
//                    VCardEngine vcardEngine = new VCardEngine();
//                    VCard vcard = vcardEngine.parse( response.text()  );
//                    LOG.info( "VCard: %s", vcard );
//                    LOG.info( "VCard: %s", vcard.getFN().getFormattedName() );
//                    LOG.info( "VCard: %s", vcard.getTitle().getTitle() );
//                    LOG.info( "VCard: %s", vcard.getEmails().get( 0 ).getEmail() );
//                });
//    }


    @Test
    @Skip
    public Promise<?> downloadAllTest() {
        var query = "<card:addressbook-query xmlns:d=\"DAV:\" xmlns:card=\"urn:ietf:params:xml:ns:carddav\">\n"
                + "    <d:prop>\n"
                + "      <d:getetag />\n"
                + "      <card:address-data />\n"
                + "    </d:prop>\n"
                + "  </card:addressbook-query>";

        return Platform.xhr( "REPORT", ARECA_CONTACTS )
                .authenticate( ARECA_USERNAME, ARECA_PWD )
                .addHeader( "Content-Type", "application/xml; charset=utf-8" )
                .addHeader( "Depth", "1" )
                .submit( query )
                .onSuccess( response -> {
                    LOG.info( "Status: %s", response.status() );
                    System.out.println( response.text() );
                    Assert.that( response.status() < 299, "Wrong status: " + response.status() );
                });

//        REPORT /addressbooks/johndoe/contacts/ HTTP/1.1
//        Depth: 1
//        Content-Type: application/xml; charset=utf-8

//        <card:addressbook-query xmlns:d="DAV:" xmlns:card="urn:ietf:params:xml:ns:carddav">
//            <d:prop>
//                <d:getetag />
//                <card:address-data />
//            </d:prop>
//        </card:addressbook-query>
    }
}
