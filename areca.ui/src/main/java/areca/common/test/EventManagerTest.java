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

import areca.common.event.EventHandler;
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


    @Test
    public void simpleTest() {
        EventManager em = new SameStackEventManager();
        em.subscribe( (Event1 ev) -> LOG.info( "Event: " + ev ) ).performIf( ev -> ev instanceof Event1 );
        em.publish( new Event2( null ) );
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
