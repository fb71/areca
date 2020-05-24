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

import java.util.List;
import java.util.stream.Collectors;

import areca.common.Assert;
import areca.common.NullProgressMonitor;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.testrunner.After;
import areca.common.testrunner.Before;
import areca.common.testrunner.Test;
import areca.systemservice.client.FolderEntry;
import areca.systemservice.client.HierarchyVisitor;
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
    public void done() {
        client.close();
    }


    @Test
    public void pathTest() {
        Assert.isEqual( 0, Path.parse( "" ).length() );
        Assert.isEqual( 0, Path.parse( Path.DELIMITER ).length() );

        Path p1 = Path.parse( "/first/second" );
        Assert.isEqual( 2, p1.length() );
        Assert.isEqual( "first", p1.part( 0 ) );
        Assert.isEqual( "second", p1.part( 1 ) );

        Path p2 = p1.plus( "third" );
        Assert.isEqual( 3, p2.length() );
        Assert.isEqual( "first", p2.part( 0 ) );
        Assert.isEqual( "third", p2.part( 2 ) );

        Path p3 = p1.plusPath( "2/3" );
        Assert.isEqual( 4, p3.length() );
        Assert.isEqual( "first", p3.part( 0 ) );
        Assert.isEqual( "3", p3.part( 3 ) );

        Assert.isEqual( 1, p1.stripFirst( 1 ).length() );
    }


    @Test
    public void requestTest() {
        List<Path> result = client.fetchFolder( basePath,
                entries -> Sequence.of( entries ).transform( entry -> entry.path ).collect( Collectors.toList() ),
                e -> { log.warn( e.toString() ); })
                .waitAndGet();

        log.info( "" + result.toString() );
        Assert.isEqual( 6, result.size() );
    }


    @Test
    public void hierarchyVisitorTest() {
        client.process( basePath.plusPath( "INBOX/messages" ), new TestHierachyVisitor(), new NullProgressMonitor() );
    }


    static class TestHierachyVisitor extends HierarchyVisitor {
        @Override
        public boolean visitFolder( Path path, List<FolderEntry> entries ) {
            log.info( "visit: " + path );
            return true;
        }
        @Override
        public void visitFile( Path path, Object content ) {
            log.info( "visit: " + path );
        }
        @Override
        public void onError( Exception e ) {
            throw new RuntimeException( "not yet implemented." );
        }
    }

}
