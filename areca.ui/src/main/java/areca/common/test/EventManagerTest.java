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

import java.util.EventObject;
import java.util.logging.Logger;

import areca.common.Assert;
import areca.common.event.EventHandler;
import areca.common.event.EventListener;
import areca.common.event.EventManager;
import areca.common.event.SameStackEventManager;
import areca.common.testrunner.Test;

/**
 *
 * @author Falko Bräutigam
 */
@Test
public class EventManagerTest {

    private static final Logger LOG = Logger.getLogger( EventManagerTest.class.getName() );

    EventObject handled = null;


    protected EventManager setupEventManager() {
        EventManager em = new SameStackEventManager();
        em.defaultOnError = e -> { throw (RuntimeException)e; };
        return em;
    }


    @Test
    public void simpleTest() {
        EventManager em = setupEventManager();

        handled = null;
        em.subscribe( (Event1 ev) -> handled = ev );
        Event1 ev = new Event1( null );
        em.publishAndWait( ev );

        Assert.isSame( ev, handled, "not same" );
    }


    @Test
    public void performIfTest() {
        EventManager em = setupEventManager();
        handled = null;
        em.subscribe( (Event1 ev) -> handled = ev ).performIf( ev -> ev instanceof Event1 );
        Event1 ev = new Event1( null );
        em.publishAndWait( ev );
        Assert.isSame( ev, handled, "not same" );
    }


    @Test
    public void performIfFalseTest() {
        EventManager em = setupEventManager();
        handled = null;
        em.subscribe( (Event1 ev) -> handled = ev ).performIf( ev -> false );
        em.publishAndWait( new Event1( null ) );
        Assert.isNull( handled );
    }


    @Test(expected = IllegalStateException.class)
    public void multiSubscribeTest() {
        EventManager em = setupEventManager();
        EventListener<Event1> l = (Event1 ev) -> handled = ev;
        em.subscribe( l );
        em.subscribe( l );
    }


    @Test
    public void disposeTest() {
        EventManager em = setupEventManager();
        EventListener<Event1> l = (Event1 ev) -> handled = ev;
        em.subscribe( l ).disposeIf( ev -> true );
        em.publishAndWait( new Event1( null ) );
        em.subscribe( l ).disposeIf( ev -> true );
        Assert.isNull( handled );
    }


    protected volatile int count;

    @Test
    public void performanceTest() {
        EventManager em = setupEventManager();
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
