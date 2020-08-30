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
package areca.rt.teavm.test;

import areca.common.reflect.ClassInfo;
import areca.common.test.EventManagerTest;
import areca.common.testrunner.After;
import areca.common.testrunner.Before;
import areca.common.testrunner.Test;
import areca.rt.teavm.SetTimeoutEventManager;

/**
 *
 * @author Falko Bräutigam
 */
@Test
public class SetTimeoutEventManagerTest {

    public static final ClassInfo<SetTimeoutEventManagerTest> info = SetTimeoutEventManagerTestClassInfo.instance();

    protected EventManagerTest      delegate;

    @Before
    protected void setup() {
        delegate = new EventManagerTest();
        delegate.setup();
        delegate.em = new SetTimeoutEventManager();
    }

    @After
    protected void tearDown() {
        delegate.tearDown();
    }

    @Test
    public void simpleTest() {
        delegate.simpleTest();
    }

    @Test
    public void performIfTest() {
        delegate.performIfTest();
    }

    @Test
    public void performIfFalseTest() {
        delegate.performIfFalseTest();
    }

    @Test(expected = IllegalStateException.class)
    public void multiSubscribeTest() {
        delegate.multiSubscribeTest();
    }

    @Test
    public void disposeTest() {
        delegate.disposeTest();
    }

    int count = 0;

    @Test
    public void performanceTest() {
        delegate.performanceTest();

//        for (int i=0; i<10; i++) {
//            delegate.em.subscribe( (EventObject ev) -> count++ )
//                    .performIf( ev -> ev instanceof EventObject )
//                    .disposeIf( ev -> false );
//        }
//        for (int i=0; i<10000; i++) {
//            delegate.em.publish( new EventObject( null ) );
//        }
//
//        Thread.yield();
//        try {
//            Thread.sleep( 1000 );
//        }
//        catch (InterruptedException e) {
//            // XXX Auto-generated catch block
//        }
    }

    @Test
    public void eventCascadeTest() {
        delegate.eventCascadeTest();
    }

    @Test
    public void newHandlerInHandlerTest() {
        delegate.newHandlerInHandlerTest();
    }

}
