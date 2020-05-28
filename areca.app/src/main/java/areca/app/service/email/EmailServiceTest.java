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

import org.polymap.model2.runtime.UnitOfWork;

import areca.app.Main;
import areca.common.NullProgressMonitor;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.testrunner.After;
import areca.common.testrunner.Before;
import areca.common.testrunner.Skip;
import areca.common.testrunner.Test;
import areca.systemservice.client.Path;
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


    @Before
    public void setup() {
        client = SystemServiceClient.connect( "webdav/" );
    }

    @After
    public void done() {
        client.close();
    }


    @Test
    public void importTest() {
        EmailFolderSynchronizer synchronizer = new EmailFolderSynchronizer();
        client.process( EMAIL_ROOT.plusPath( "Test1/messages" ), synchronizer, new NullProgressMonitor() )
                .waitAndGet();
        try (
            UnitOfWork uow = Main.repo.supply().newUnitOfWork();
        ){
            //uow.query( Message.class ).where( );
        }
    }

    @Test
    @Skip
    public void import2Test() {

    }

}
