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
package areca.common.test;

import static areca.common.Platform.async;

import org.apache.commons.lang3.mutable.MutableInt;

import areca.common.Assert;
import areca.common.AssertionException;
import areca.common.AsyncJob;
import areca.common.Platform;
import areca.common.Promise;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.RuntimeInfo;
import areca.common.testrunner.Skip;
import areca.common.testrunner.Test;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class AsyncTests {

    private static final Log LOG = LogFactory.getLog( AsyncTests.class );

    public static final AsyncTestsClassInfo info = AsyncTestsClassInfo.instance();

    @Test
    @Skip
    public void asyncJobTest() throws Exception {
        var monitor = new Object();
        new AsyncJob()
                .schedule( "work 1", site -> {
                    LOG.info( "work 1" );
                })
                .schedule( "work 2", site -> {
                    LOG.info( "work 2" );
                    synchronized (monitor) {
                        monitor.notifyAll();
                    }
                })
                .start();

        synchronized (monitor) {
            monitor.wait( 1000 );
        }
    }


    @Test
    public Promise<Integer> cascadedPromiseTest() {
        return async( () -> {
                    return "1";
                })
                .then( s -> {
                    return Platform.async( () -> Integer.valueOf( s ) );
                })
                .onSuccess( i -> {
                    LOG.info( "Result: " + i );
                    Assert.isEqual( 1, i );
                });
    }


    @Test
    public Promise<Integer> spreadPromiseTest() {
        MutableInt count = new MutableInt( 0 );
        return async( () -> {
                    return 2;
                })
                .then( num -> {
                    return Promise.joined( num, i -> Platform.async( () -> i ) );
                })
                .onSuccess( i -> {
                    LOG.debug( "Result: " + i );
                    Assert.that( count.getAndIncrement() < 2 );
                });
    }


    @Test(expected = AssertionException.class)
    public Promise<?> promiseError() {
        return async( () -> {
                    Assert.that( 1==2, "..." );
                    return "1";
                })
//                .onError( e -> {
//                    LOG.info( "Error: %s", e );
//                    Assert.isEqual( 1, e );
//                })
                .onSuccess( i -> {
                    LOG.info( "Result: " + i );
                });
    }


    @Test(expected = AssertionException.class)
    public Promise<?> promiseHandlerError() {
        return Platform
                .async( () -> {
                    return "1";
                })
                .onSuccess( i -> {
                    LOG.info( "Result: " + i );
                    Assert.isEqual( "falsch", i );
                });
    }


    @Test(expected = AssertionException.class)
    public Promise<Integer> cascadedPromiseError() {
        return async( () -> {
                    Assert.isEqual( 1, 2, "..." );
                    return "1";
                })
                .then( s -> {
                    return Platform.async( () -> Integer.valueOf( s ) );
                })
                .onSuccess( i -> {
                    LOG.info( "Result: " + i );
                });
    }


    @Test(expected = AssertionException.class)
    public Promise<Integer> cascadedPromiseHandlerError() {
        return async( () -> {
                    return "1";
                })
                .then( s -> {
                    return Platform.async( () -> {
                        Assert.isEqual( "falsch", s );
                        return Integer.valueOf( s );
                    });
                })
                .onError( e -> {
                    LOG.info( "Error: " + e );
                })
                .onSuccess( i -> {
                    LOG.info( "Result: " + i );
                });
    }


    @Test
    public Promise<?> multipleValuePromiseTest() {
        MutableInt count = new MutableInt();
        return Sequence.ofInts( 1, 100 )
                .map( i -> Platform.async( () -> i ) )
                .reduce( (p1, p2) -> p1.join( p2 ) ).get()
                .onSuccess( (promise,i) -> {
                    count.increment();
                    if (promise.isComplete()) {
                        Assert.isEqual( 100, count.getValue() );
                    }
                });
    }


    @Test
    public Promise<?> reducePromiseTest() {
        return Promise
                .joined( 3, i -> Platform.async( () -> i ) )
                .reduce( new MutableInt(), (r,i) -> r.add( i ) )
                .onSuccess( (self,count) -> {
                    if (self.isComplete()) {
                        Assert.isEqual( 3, count.getValue() );
                    }
                });
    }


    @Test
    public Promise<?> filterPromiseTest() {
        return Promise
                .joined( 4, i -> Platform.async( () -> i ) )
                .filter( i -> i > 2 )
                .onSuccess( (self,i) -> {
                    Assert.that( i > 2 );
                });
    }


    @Test
    public void simpleSuccess() {
    }


    @Test(expected = AssertionException.class)
    public void simpleError() {
        Assert.isEqual( 1, 2, "..." );
    }
}
