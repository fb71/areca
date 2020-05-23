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
package areca.systemservice.client.test;

import java.util.concurrent.Future;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.testrunner.After;
import areca.common.testrunner.Before;
import areca.common.testrunner.Test;
import areca.systemservice.client.Path;
import areca.systemservice.client.SystemServiceClient;

/**
 *
 * @author Falko Bräutigam
 */
@Test
public class ReadTest {

    private static final Log log = LogFactory.getLog( ReadTest.class );

    public static final ReadTestClassInfo info = ReadTestClassInfo.instance();

    private static final Path basePath = Path.parse( "support@polymap.de" );

    protected SystemServiceClient       client;


    @Before
    public void setup() {
        client = SystemServiceClient.connect( "webdav/" );
    }

    @After
    public void dispose() {
        client.close();
    }


    @Test
    public void hierarchyVisitorTest() {
        client.process( basePath.plusPath( "INBOX/messages" ), new Test1Visitor(),  );
    }


    @Test
    public void requestTest() {
        client.fetchFolder( basePath,
                entries -> {

                },
                e -> {
                    log.warn( "creating test data..." );
                });
    }

}
