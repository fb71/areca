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

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import areca.common.Assert;
import areca.common.Promise;
import areca.common.event.EventHandler;
import areca.common.event.EventListener;
import areca.common.event.EventManager;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.testrunner.After;
import areca.common.testrunner.Before;
import areca.common.testrunner.Test;

/**
 *
 * @author Falko Bräutigam
 */
@Test
public class EventManagerTest {

    private static final Log LOG = LogFactory.getLog( EventManagerTest.class );

    public static final ClassInfo<EventManagerTest> info = EventManagerTestClassInfo.instance();

    public EventManager         em;

    protected EventObject       handled;

    protected volatile int      count;


    @Before
    protected void setup() {
        em.defaultOnError = (ev,e) -> { throw (RuntimeException)e; };
    }

    @After
    protected void tearDown() {
    }


    @Test
    public Promise<Void> simpleTest() {
        handled = null;
        em.subscribe( ev -> handled = ev );
        Event1 ev = new Event1();
        return em.publish2( ev ).onSuccess( __ -> {
            Assert.isSame( ev, handled, "not same" );
        });
    }


    @Test
    public void simpleSyncTest() {
        handled = null;
        em.subscribe( (Event1 ev) -> handled = ev );
        Event1 ev = new Event1();
        em.publish2( ev ).waitForResult();
        Assert.isSame( ev, handled, "not same" );
    }


    @Test
    public Promise<Void> performIfTest() {
        handled = null;
        em.subscribe( (Event1 ev) -> handled = ev ).performIf( ev -> ev instanceof Event1 );
        Event1 ev = new Event1();
        return em.publish2( ev ).onSuccess( __ -> {
            Assert.isSame( ev, handled, "not same" );
        });
    }


    @Test
    public Promise<Void> performIfFalseTest() {
        handled = null;
        em.subscribe( (Event1 ev) -> handled = ev ).performIf( ev -> ev instanceof Event1 );
        return em.publish2( new Event2() ).onSuccess( __ -> {
            LOG.info( em.getClass().getSimpleName() + ": performIfFalseTest: OK" );
            Assert.isNull( handled );
        });
    }


    @Test(expected = IllegalStateException.class)
    public void multiSubscribeTest() {
        EventListener<Event1> l = (Event1 ev) -> handled = ev;
        em.subscribe( l );
        em.subscribe( l );
    }


    @Test
    public Promise<Void> disposeTest() {
        EventListener<Event1> l = (Event1 ev) -> handled = ev;
        em.subscribe( l ).unsubscribeIf( () -> true );
        return em.publish2( new Event1() ).onSuccess( __ -> {
            em.subscribe( l ).unsubscribeIf( () -> true );
            Assert.isNull( handled );
        });
    }


    @Test
    public Promise<Void> performanceTest() {
        count = 0;
        for (int i=0; i<100; i++) {
            em.subscribe( (Event1 ev) -> count++ )
                    .performIf( ev -> ev instanceof Event1 )
                    .unsubscribeIf( () -> false );
        }
        for (int i=0; i<1000; i++) {
            em.publish( new Event1() );
        }
        return em.publish2( new Event1() ).onSuccess( __ -> {
            LOG.info( "count: " + count );
            Assert.isEqual( 100100, count, "" );
        });
    }


    @Test
    public Promise<Void> eventCascadeTest() {
        List<EventObject> caught = new ArrayList<>();

        // catch Event1
        em.subscribe( ev -> {
            caught.add( ev );
            em.publish2( new Event2( null ) ).onSuccess( __ -> {
                // check outside test result Promise
                Assert.that( caught.get( 0 ) instanceof Event1 );
                Assert.that( caught.get( 1 ) instanceof Event2 );
                Assert.isEqual( 2, caught.size() );
                LOG.info( em.getClass().getSimpleName() + ": eventCascadeTest: OK" );
            });
        })
        .performIf( ev -> ev instanceof Event1 );

        // catch Event2
        em.subscribe( ev -> {
            caught.add( ev );
        })
        .performIf( ev -> ev instanceof Event2 );

        return em.publish2( new Event1() ).onSuccess( __ -> {
            Assert.that( caught.get( 0 ) instanceof Event1 );
            // for SameStack this would fail
            // Assert.isEqual( 1, caught.size() );
        });
    }


    @Test
    public Promise<Void> newHandlerInHandlerTest() {
        List<EventObject> caught = new ArrayList<>();

        em.subscribe( ev -> {
            LOG.info( "Handler1: " + ev.getClass().getSimpleName() );
            em.subscribe( _ev -> {
                LOG.info( "Handler2: " + _ev.getClass().getSimpleName() );
                caught.add( _ev );
            });
        }).performIf( ev -> ev instanceof Event1 );

        return em.publish2( new Event1() )
                .then( __ -> {
                    Assert.that( caught.isEmpty() );
                    return em.publish2( new Event2() );
                })
                .onSuccess( __ -> {
                    Assert.that( caught.get( 0 ) instanceof Event2, "Event2: " + caught.get( 0 ) + " (" + caught );
                });
    }


    //@Skip
    @Test(expected = RuntimeException.class)
    public void test() {
        em.subscribe( ev -> {
            System.out.println( "THREAD: " + Thread.currentThread() );
            throwSomething();
        });
        em.publish( new EventObject( null ) );
    }


    protected void throwSomething() {
        RuntimeException e = new RuntimeException();
        e.fillInStackTrace();
        throw e;
    }


    static class Event1 extends EventObject {
        public Event1() { super( null ); }
        public Event1( Object source ) { super( source ); }
    }


    static class Event2 extends EventObject {
        public Event2() { super( null ); }
        public Event2( Object source ) { super( source ); }
    }


    @EventHandler( EventObject.class )
    protected void handler( EventObject o ) {

    }
}
