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


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import areca.common.Platform;
import areca.common.Session;
import areca.common.SessionScoper;
import areca.common.SessionScoper.ThreadBoundSessionScoper;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
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
class JUnitTestBase {

    private static final Log LOG = LogFactory.getLog( JUnitTestBase.class );

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
        sessionScope.unbind( session );
        session.dispose();
        session = null;
    }

    protected void execute( ClassInfo<?>... tests ) {
        execute( () -> new TestRunner().addTests( tests ).addDecorators( LogDecorator.info ).run() );
    }

    protected void execute( Runnable task ) {
        var eventLoop = Session.instanceOf( EventLoop.class );
        eventLoop.enqueue( "JUnitTest", task, 0 );
        eventLoop.execute();
    }

}
