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

import areca.common.Assert;
import areca.common.AssertionException;
import areca.common.MutableInt;
import areca.common.Platform;
import areca.common.Promise;
import areca.common.Promise.CancelledException;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.RuntimeInfo;
import areca.common.testrunner.Test;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class AsyncTests {

    private static final Log LOG = LogFactory.getLog( AsyncTests.class );

    public static final AsyncTestsClassInfo info = AsyncTestsClassInfo.instance();


    @Test(expected = CancelledException.class)
    public Promise<?> cancelPromiseTest() {
        var result = async( () -> "1" )
                .then( s -> async( () -> Integer.valueOf( s ) ) );
        result.cancel();
        Assert.isEqual( true, result.isCanceled() );
        return result;
    }


    @Test(expected = CancelledException.class)
    public Promise<?> cancelJoinedPromiseTest() {
        var result = Promise.joined( 10, i -> async( () -> i ) )
                .reduce2( 0, (r,next) -> r += next );
        result.cancel();
        Assert.isEqual( true, result.isCanceled() );
        return result;
    }


    @Test(expected = CancelledException.class)
    public Promise<?> cancelFromWithinJoinedPromiseTest() {
        return Promise.joined( 10, i -> async( () -> i ) )
                .map( (i,self) -> {
                    self.cancel();
                    self.complete( i );
                })
                .onSuccess( value -> {
                    throw new IllegalStateException( "Result after error" );
                });
    }


    @Test(expected = AssertionException.class)
    public Promise<?> cancelAfterErrorTest() {
        return Promise.joined( 10, i -> async( () -> i ) )
                .reduce2( 0, (r,next) -> Assert.isNull( next, "never null" ) )
                .onSuccess( value -> {
                    throw new IllegalStateException( "Result after error" );
                });
    }


    @Test(expected = AssertionException.class)
    public Promise<?> cancelAfterErrorTest2() {
        return Promise.joined( 10, i -> async( () -> Assert.isNull( i, "never null" ) ) )
                .reduce2( 0, (r,next) -> r + next )
                .onSuccess( value -> {
                    throw new IllegalStateException( "Result after error" );
                });
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
    public Promise<?> cascadedPromiseHandlerError() {
        return async( () -> "1" )
                .then( s -> async( () -> {
                    Assert.isEqual( "falsch", s );
                    return Integer.valueOf( s );
                }))
                .onSuccess( value -> {
                    throw new IllegalStateException( "Result after error" );
                });
    }


    @Test
    public Promise<?> promiseThenTest() {
        return Promise
                .joined( 3, i -> Platform.async( () -> i ) )
                .then( i -> async( () -> i ) )
                .onSuccess( i -> {
                    Assert.isEqual( 3, i );
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
                    Assert.isEqual( 3, count.getValue() );
                });
    }


    @Test
    public Promise<?> reduce2PromiseTest() {
        return Promise
                .joined( 4, i -> Platform.async( () -> i ))
                .reduce2( 0, (r,i) -> r + i )
                .onSuccess( (self,count) -> {
                    Assert.isEqual( 6, count );
                });
    }


    @Test
    public Promise<?> joinedSerialTest() {
        return Promise
                .serial( 4, i -> Platform.async( () -> {
                    LOG.info( "JOIN: " + i );
                    return i;
                }))
                .onSuccess( i -> LOG.info( "JOIN: success: %s", i ) )
                .reduce2( 0, (r,i) -> r + i )
                .onSuccess( count -> {
                    Assert.isEqual( 6, count.intValue() );
                });
    }


    @Test
    public Promise<?> propagateOptionalPromiseTest() {
        return Promise.absent()
                .thenOpt( v -> (Platform.async( () -> Assert.notNull( v.get() ) )) )
                .reduce2( 0, (r,i) -> r + 1 )
                .onSuccess( (self,count) -> {
                    Assert.isEqual( 1, count );
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
