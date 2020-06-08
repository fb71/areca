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
package areca.app.service.email;

import java.util.Arrays;

import org.polymap.model2.query.ResultSet;
import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.store.tidbstore.IDBStore;
import areca.app.model.Anchor;
import areca.app.model.Contact;
import areca.app.model.Message;
import areca.common.Assert;
import areca.common.NullProgressMonitor;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.testrunner.After;
import areca.common.testrunner.Before;
import areca.common.testrunner.Test;
import areca.systemservice.client.Path;
import areca.systemservice.client.SimpleDocument;
import areca.systemservice.client.SystemServiceClient;

/**
 *
 * @author Falko Bräutigam
 */
@Test
public class EmailServiceTest {

    private static final Log log = LogFactory.getLog( EmailServiceTest.class );

    public static final EmailServiceTestClassInfo info = EmailServiceTestClassInfo.instance();

    private static final Path EMAIL_ROOT = Path.parse( "areca@polymap.de" );

    protected SystemServiceClient       client;

    protected static EntityRepository   repo;

    static {
        log.info( "Creating repo..." );
        repo = EntityRepository.newConfiguration()
                .entities.set( Arrays.asList( Message.info, Contact.info, Anchor.info) )
                .store.set( new IDBStore( "main2" /*EmailServiceTest.class.getSimpleName()*/, 1, true ) )
                .create();
    }

    @Before
    public void setup() {
        client = SystemServiceClient.connect( "webdav/" );
    }

    @After
    public void done() {
        client.close();
    }


    @Test
    public void domParserTest() {
        SimpleDocument doc = SimpleDocument.parseXml( "<start><child></child><child></child></start>" );
        Assert.isEqual( 1, doc.getElementsByTagName( "start" ).size() );
        Assert.isEqual( 2, doc.getElementsByTagName( "child" ).size() );
    }


    @Test
    public void importTest() throws Exception {
        EmailFolderSynchronizer synchronizer = new EmailFolderSynchronizer();
        client.process( EMAIL_ROOT.plusPath( "Test1/messages" ), synchronizer, new NullProgressMonitor() )
                .waitAndGet();
        synchronizer.processEnvelopes( repo, new NullProgressMonitor() );

        synchronizer.exception.<Exception>ifPresent( e -> {
            throw e;
        });

        try (
            UnitOfWork uow = repo.newUnitOfWork();
        ){
            ResultSet<Message> rs = uow.query( Message.class ).execute();
            Assert.isEqual( 17, rs.size() );
            for (Message message : rs) {
                log.info( "Message - From: " + message.from.get() );
            }
        }
    }

}
