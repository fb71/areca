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
package areca.common.test;

import areca.common.Assert;
import areca.common.Promise;
import areca.common.event.UIEventManager;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.testrunner.Before;
import areca.common.testrunner.Skip;
import areca.common.testrunner.Test;

/**
 *
 * @author Falko Bräutigam
 */
@Test
public class UIEventManagerTest
        extends EventManagerTest {

    private static final Log LOG = LogFactory.getLog( UIEventManagerTest.class );

    @SuppressWarnings("hiding")
    public static final ClassInfo<UIEventManagerTest> info = UIEventManagerTestClassInfo.instance();

    @Before
    protected void setup() {
        em = new UIEventManager();
        super.setup();
    }

    @Test @Skip
    @Override
    public void test() {
        throw new RuntimeException( "just skip this test" );
    }

    @Test @Skip
    @Override
    public Promise<Void> performanceTest() {
        throw new RuntimeException( "not yet implemented." );
    }

    @Test
    public Promise<Void> performanceTest2() {
        var handlers = 10;
        var events = 10;
        LOG.info( "Handlers: %s, events: %s", handlers, events );

        count = 0;
        for (int i=0; i<handlers; i++) {
            em.subscribe( (Event1 ev) -> count++ )
                    .performIf( ev -> ev instanceof Event1 )
                    .unsubscribeIf( () -> false );
        }
        for (int i=0; i<events; i++) {
            em.publish( new Event1() );
        }
        return em.publish2( new Event1() ).onSuccess( __ -> {
            LOG.info( "count: " + count );
            Assert.isEqual( (events + 1) * handlers, count, "" );
        });
    }

}
