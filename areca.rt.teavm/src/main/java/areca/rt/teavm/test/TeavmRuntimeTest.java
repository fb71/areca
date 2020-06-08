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

import org.teavm.jso.ajax.XMLHttpRequest;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.testrunner.Test;

/**
 *
 * @author Falko Bräutigam
 */
@Test
public class TeavmRuntimeTest {

    private static final Log log = LogFactory.getLog( TeavmRuntimeTest.class );

    public static TeavmRuntimeTestClassInfo info = TeavmRuntimeTestClassInfo.instance();


    @Test
    public void threadInAsyncHandlerTest() throws InterruptedException {
        synchronized (this) {
            wait( 500 );
        }

//        new Timer().schedule( new TimerTask() {
//            @Override
//            public void run() {
//                log.info( "Thread: " + Thread.currentThread() );
//                try {
//                    synchronized (this) {
//                        wait( 3000 );
//                    }
//                }
//                catch (InterruptedException e) { }
//                log.info( "Thread: " + Thread.currentThread() );
//            }
//        }, 500);

        XMLHttpRequest request1 = XMLHttpRequest.create();
        request1.open( "GET", "index.html", true );
        request1.onComplete( () -> {
            log.info( "Thread: " + Thread.currentThread() );
            new Thread( () -> {
                log.info( "Thread: " + Thread.currentThread() );
                try {
                    synchronized (this) {
                        wait( 500 );
                    }
                }
                catch (InterruptedException e) {
                }
            }).start();
        });
        request1.send();
    }

}
