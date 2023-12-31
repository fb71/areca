package areca.rt.server.test;
/*
 * Copyright (C) 2023, the @authors. All rights reserved.
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


import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import areca.common.Assert;
import areca.common.Platform;
import areca.common.Session;
import areca.common.SessionScoper;
import areca.common.SessionScoper.ThreadBoundSessionScoper;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.testrunner.LogDecorator;
import areca.common.testrunner.TestRunner;
import areca.rt.server.EventLoop;
import areca.rt.server.ServerPlatform;

/**
 * JUnit test runner for Areca core tests, running inside the JVM.
 *
 * @author Falko Bräutigam
 */
@SuppressWarnings("unchecked")
class JUnitTest {

    private static final Log LOG = LogFactory.getLog( JUnitTest.class );

    private static ThreadBoundSessionScoper sessionScope = new ThreadBoundSessionScoper();

    private Session session;

    @BeforeAll
    protected static void setup() {
        Platform.impl = new ServerPlatform();
        SessionScoper.setInstance( sessionScope );
    }

    @BeforeEach
    protected void bindSession() {
        session = new Session();
        sessionScope.bind( session );
        Assertions.assertSame( session, Session.current() );
        Session.setInstance( new EventLoop() );
    }

    @AfterEach
    protected void unbindSession() {
        // execute the EventLoop after each test
        Session.instanceOf( EventLoop.class ).execute( -1 );

        sessionScope.unbind( session );
        session.dispose();
        session = null;
    }

    // tests **********************************************

    @Test
    void sequenceTest() {
        new TestRunner()
                .addTests( areca.common.test.SequenceTest.info )
                .addDecorators( LogDecorator.info )
                .run();
    }

    @Test
    void sequenceOpTest() {
        new TestRunner()
                .addTests( areca.common.test.SequenceOpTest.info )
                .addDecorators( LogDecorator.info )
                .run();
    }

    @Test
    public void simpleAsyncTest() {
        var flag = new AtomicBoolean();
        Platform.async( () -> flag.set( true ) );

        Session.instanceOf( EventLoop.class ).execute( -1 );
        Assert.that( flag.get()  );
    }


    @Test
    public void simpleAsyncSuccessTest() {
        var flag = new AtomicBoolean();
        Platform.async( () -> null )
                .onSuccess( __ -> flag.set( true ) );

        Session.instanceOf( EventLoop.class ).execute( -1 );
        Assert.that( flag.get()  );
    }


    @Test
    public void xhrTest() throws InterruptedException {
        new TestRunner()
                .addTests( areca.common.test.XhrTest.info )
                .addDecorators( LogDecorator.info )
                .run();

//        Thread.sleep( 1000 );
//        Session.instanceOf( EventLoop.class ).execute( -1 );
//        Assert.that( tests.isComplete() );
    }


    @Test
    public void asyncTest() {
        new TestRunner()
                .addTests( areca.common.test.AsyncTests.info )
                .addDecorators( LogDecorator.info )
                .run();
    }


    @Test
    public void runtimeTest() {
        new TestRunner()
                .addTests( areca.common.test.RuntimeTest.info )
                .addDecorators( LogDecorator.info )
                .run();
    }

//    @Test
//    public void schedulerTest() {
//        new AsyncTestRunner()
//                .addTests( areca.common.test.SchedulerTest.info )
//                .addDecorators( LogDecorator.info )
//                .run();
//    }

    @Test
    public void asyncEventManagerTest() {
        new TestRunner()
                .addTests( areca.common.test.AsyncEventManagerTest.info )
                .addDecorators( LogDecorator.info )
                .run();
    }

    @Test
    public void idleAsyncEventManagerTest() {
        new TestRunner()
                .addTests( areca.common.test.IdleAsyncEventManagerTest.info )
                .addDecorators( LogDecorator.info )
                .run();
    }

    @Test
    public void sameStackEventManagerTest() {
        new TestRunner()
                .addTests( areca.common.test.SameStackEventManagerTest.info )
                .addDecorators( LogDecorator.info )
                .run();
    }

    @Test
    public void uiEventManagerTest() {
        new TestRunner()
                .addTests( areca.ui.test.UIEventManagerTest.info )
                .addDecorators( LogDecorator.info )
                .run();
    }

    void test() {
        new TestRunner()
                // .addTests( areca.common.test.UIEventManagerTest.info )
                // .addTests( areca.rt.teavm.test.TeavmRuntimeTest.info )
                .addTests( areca.common.test.RuntimeTest.info )
                // .addTests( areca.common.test.AsyncTests.info )
                //.addTests( areca.common.test.SchedulerTest.info )
                // .addTests( areca.common.test.IdleAsyncEventManagerTest.info )
                // .addTests( areca.common.test.AsyncEventManagerTest.info )
                .addDecorators( LogDecorator.info )
                .run();

//        fail( "Not yet implemented" );
    }

}
