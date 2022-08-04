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
import areca.common.Platform;
import areca.common.Promise;
import areca.common.Scheduler;
import areca.common.Scheduler.Priority;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.common.testrunner.Skip;
import areca.common.testrunner.Test;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class SchedulerTest {

    private static final Log LOG = LogFactory.getLog( SchedulerTest.class );

    public static final ClassInfo<SchedulerTest> info = SchedulerTestClassInfo.instance();

    protected static Scheduler scheduler = new Scheduler();

    @Test
    public Promise<?> simpleTest() {
        return scheduler.schedule( /*Priority.BACKGROUND, 10000,*/ () -> "0" );
    }


    @Skip
    @Test
    public Promise<?> priorityTest() {
        var result = scheduler.schedule( Priority.DECORATION, () -> "deco" );
        result = result.join( scheduler.schedule( Priority.BACKGROUND, () -> "background" ) );
        result = result.join( scheduler.schedule( Priority.INTERACTIVE, () -> "interactive" ) );
        result = result.join( scheduler.schedule( Priority.DECORATION, () -> "deco2" ) );
        return result.reduce2( "", (r,next) -> r + " " + next )
                .onSuccess( r -> {
                    LOG.info( "Result: " + r );
                    Assert.isEqual( " interactive background deco deco2", r );
                });
    }


    @Test
    public Promise<?> joinedTest() {
        return Promise.joined( 200, i -> {
            return scheduler.schedule( () -> "0" );
        });
    }


    @Test
    public Promise<?> prioritizedPromiseTest() {
        return Platform.async( () -> "0" )
                .priority( Priority.BACKGROUND )
                .onSuccess( result -> {
                    Assert.isEqual( "0", result );
                });
    }


    @Test
    public Promise<?> prioritizedXhrTest() {
        return Platform.xhr( "GET", "index.html" )
                .submit()
                .priority( Priority.BACKGROUND )
                .onSuccess( response -> {
                    Assert.isEqual( 200, response.status() );
                });
    }

}
