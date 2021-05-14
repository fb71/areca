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

import areca.common.Assert;
import areca.common.AssertionException;
import areca.common.AsyncJob;
import areca.common.Platform;
import areca.common.Promise;
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
        return Platform.instance()
                .async( () -> {
                    return "1";
                })
                .then( s -> {
                    return Platform.instance().async( () -> Integer.valueOf( s ) );
                })
                .onSuccess( i -> {
                    LOG.info( "Result: " + i );
                    Assert.isEqual( 1, i );
                });
    }


    @Test(expected = AssertionException.class)
    public Promise<?> promiseError() {
        return Platform.instance()
                .async( () -> {
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
        return Platform.instance()
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
        return Platform.instance()
                .async( () -> {
                    Assert.that( 1==2, "..." );
                    return "1";
                })
                .then( s -> {
                    return Platform.instance().async( () -> Integer.valueOf( s ) );
                })
                .onSuccess( i -> {
                    LOG.info( "Result: " + i );
                });
    }


    @Test(expected = AssertionException.class)
    public Promise<Integer> cascadedPromiseHandlerError() {
        return Platform.instance()
                .async( () -> {
                    return "1";
                })
                .then( s -> {
                    return Platform.instance().async( () -> {
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


    public void multipleValuePromiseTest() {

    }


    @Test
    @Skip
    public void success() {
    }
}
