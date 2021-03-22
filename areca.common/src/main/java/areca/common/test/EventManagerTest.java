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
import java.util.logging.Logger;

import areca.common.Assert;
import areca.common.event.EventHandler;
import areca.common.event.EventListener;
import areca.common.event.EventManager;
import areca.common.event.ThreadedEventManager;
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

    private static final Logger LOG = Logger.getLogger( EventManagerTest.class.getName() );

    public static final ClassInfo<EventManagerTest> info = EventManagerTestClassInfo.instance();

    public EventManager         em;

    protected EventObject       handled;

    protected volatile int      count;


    @Before
    public void setup() {
        em = new ThreadedEventManager(); //kEventManager();
        em.defaultOnError = (ev,e) -> { throw (RuntimeException)e; };
    }

    @After
    public void tearDown() {
    }


    @Test
    public void simpleTest() {
        handled = null;
        em.subscribe( (Event1 ev) -> handled = ev );
        Event1 ev = new Event1( null );
        em.publishAndWait( ev );

        Assert.isSame( ev, handled, "not same" );
    }


    @Test
    public void performIfTest() {
        handled = null;
        em.subscribe( (Event1 ev) -> handled = ev ).performIf( ev -> ev instanceof Event1 );
        Event1 ev = new Event1( null );
        em.publishAndWait( ev );
        Assert.isSame( ev, handled, "not same" );
    }


    @Test
    public void performIfFalseTest() {
        handled = null;
        em.subscribe( (Event1 ev) -> handled = ev ).performIf( ev -> false );
        em.publishAndWait( new Event1( null ) );
        Assert.isNull( handled );
    }


    @Test(expected = IllegalStateException.class)
    public void multiSubscribeTest() {
        EventListener<Event1> l = (Event1 ev) -> handled = ev;
        em.subscribe( l );
        em.subscribe( l );
    }


    @Test
    public void disposeTest() {
        EventListener<Event1> l = (Event1 ev) -> handled = ev;
        em.subscribe( l ).disposeIf( ev -> true );
        em.publishAndWait( new Event1( null ) );
        em.subscribe( l ).disposeIf( ev -> true );
        Assert.isNull( handled );
    }


    @Test
    public void performanceTest() {
        count = 0;
        for (int i=0; i<10; i++) {
            em.subscribe( (Event1 ev) -> count++ )
                    .performIf( ev -> ev instanceof Event1 )
                    .disposeIf( ev -> false );
        }
        for (int i=0; i<10000; i++) {
            em.publish( new Event1( null ) );
        }
        em.publishAndWait( new Event1( null ) );
        LOG.info( "count: " + count );
        Assert.isEqual( 100010, count, "" );
    }


    @Test
    public void eventCascadeTest() {
        List<EventObject> caught = new ArrayList<>();
        em.subscribe( ev -> {
            caught.add( ev );
            em.publishAndWait( new Event2( null ) );
        })
        .performIf( ev -> ev instanceof Event1 );

        em.subscribe( ev -> {
            caught.add( ev );
        })
        .performIf( ev -> ev instanceof Event2 );

        em.publishAndWait( new Event1( null ) );

        Assert.that( caught.get( 0 ) instanceof Event1 );
        Assert.that( caught.get( 1 ) instanceof Event2 );
    }


    @Test
    public void newHandlerInHandlerTest() {
        List<EventObject> caught = new ArrayList<>();
        em.subscribe( ev -> {
            em.subscribe( ev2 -> caught.add( ev ) );
        });
        em.publishAndWait( new Event1( null ) );
    }


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


    static class Event1
            extends EventObject {
        public Event1( Object source ) { super( source ); }
    }


    static class Event2
            extends EventObject {
        public Event2( Object source ) { super( source ); }
    }


    @EventHandler
    protected void handler( EventObject o ) {

    }
}
