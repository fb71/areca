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
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import areca.common.Assert;
import areca.common.AssertionException;
import areca.common.Platform;
import areca.common.Promise;
import areca.common.Promise.CancelledException;
import areca.common.Session;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.rt.server.EventLoop;

/**
 * JUnit test runner for Areca core tests, running inside the JVM.
 *
 * @author Falko Bräutigam
 */
class JUnitTest extends JUnitTestBase {

    private static final Log LOG = LogFactory.getLog( JUnitTest.class );

    @Test
    public void sequenceTest() {
        execute( areca.common.test.SequenceTest.info );
    }

    @Test
    public void sequenceOpTest() {
        execute( areca.common.test.SequenceOpTest.info );
    }

    @Test
    public void simpleAsyncTest() {
        execute( () -> {
            var flag = new AtomicBoolean();

            var async = Platform.async( () -> flag.compareAndSet( false, true ) );
            Assert.that( !async.isCompleted()  );
            Assert.that( !async.isCanceled()  );

            async.waitForResult();
            //Session.instanceOf( EventLoop.class ).execute();
            Assert.that( async.isCompleted()  );
            Assert.that( !async.isCanceled()  );
            Assert.that( flag.get()  );
        });
    }


    @Test
    public void simpleAsyncCancelTest() {
        execute( () -> {
            var flag = new AtomicBoolean();
            var error = new AtomicReference<>();

            var async = Platform
                    .async( () -> flag.compareAndSet( false, true ) )
                    .onError( e -> error.set( Assert.isType( CancelledException.class, e ) ) )
                    .onSuccess( __ -> Assert.that( false, "must never reach this" ) );

            async.cancel();
            async.cancel();
            Assert.that( async.isCanceled()  );
            Assert.that( async.isCompleted()  );
            Assert.notNull( error.get()  );

            async.waitForResult();

            Assert.that( async.isCompleted()  );
            Assert.that( async.isCanceled()  );
            Assert.that( !flag.get()  );
        });
    }


    @Test
    public void simpleAsyncSuccessTest() {
        execute( () -> {
            var flag = new AtomicBoolean();
            Platform.async( () -> null ).onSuccess( __ -> flag.set( true ) );

            Session.instanceOf( EventLoop.class ).execute();
            Assert.that( flag.get()  );
        });
    }

    @Test
    public void promiseJoinTest() {
        execute( () -> {
            var a1 = Platform.async( () -> 1 );
            var a2 = Platform.async( () -> 2 );
            var a3 = a1.join( a2 );
            a3.waitForResult();
            Assert.that( a3.isCompleted() );
        });
    }


    @Test
    public void promiseJoinedTest() {
        execute( () -> {
            var async = Promise
                    .joined( 4, i -> Platform.async( () -> {
                        LOG.info( "JOIN: " + i );
                        return i;
                    }))
                    .onSuccess( i -> LOG.info( "JOIN: onSuccess(): %s", i ) )
                    .reduce2( 0, (r,i) -> r + i )
                    .onSuccess( count -> {
                        Assert.isEqual( 6, count.intValue() );
                    });

            async.waitForResult( result -> {
                Assert.isEqual( 6, result );
            });
        });
    }


    @Test
    public void promiseOnSuccessCompleteStateTest() {
        execute( () -> {
            var a1 = Platform.async( () -> 1 )
                    .onSuccess( (self,result) -> Assert.that( self.isComplete() ) );
            a1.waitForResult();
        });
    }


    @Test
    public void promiseOnSuccessError() {
        execute( () -> {
            var error = new AtomicReference<>();
            var async = Platform.async( () -> "1" )
                    .onSuccess( i -> {
                        LOG.info( "Result: " + i );
                        Assert.that( false, "Error in onSuccess()" );
                    })
                    .onError( e -> {
                        error.set( Assert.isType( AssertionException.class, e ) );
                    });

            async.waitForResult();
            Assert.notNull( error.get() );
        });
    }


    @Test
    public void promiseCancelFromHandlerTest() {
        execute( () -> {
            var error = new AtomicReference<>();
            Platform.async( () -> 1 )
                    .map( ( i, self ) -> {
                        self.cancel();
                        self.complete( i );
                    })
                    .onError( e -> {
                        error.set( Assert.isType( CancelledException.class, e ) );
                    })
                    .onSuccess( value -> {
                        throw new IllegalStateException( "Result after error" );
                    });
        });
    }



    @Test
    public void xhrTest() throws InterruptedException {
        execute( areca.common.test.XhrTest.info );
    }


    @Test
    public void asyncTest() {
        execute( areca.common.test.AsyncTests.info );
    }


    @Test
    public void runtimeTest() {
        execute( areca.common.test.RuntimeTest.info );
    }

    @Test
    public void schedulerTest() {
        execute( areca.common.test.SchedulerTest.info );
    }

    @Test
    public void asyncEventManagerTest() {
        execute( areca.common.test.AsyncEventManagerTest.info );
    }

    @Test
    public void idleAsyncEventManagerTest() {
        execute( areca.common.test.IdleAsyncEventManagerTest.info );
    }

    @Test
    public void sameStackEventManagerTest() {
        execute( areca.common.test.SameStackEventManagerTest.info );
    }

    @Test
    public void annotationTest() {
        execute( areca.common.test.AnnotationTest.info );
    }

    @Test
    public void uiEventManagerTest() {
        execute( areca.ui.test.UIEventManagerTest.info );
    }

}
