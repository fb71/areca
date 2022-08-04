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

import org.teavm.jso.JSBody;
import org.teavm.jso.JSExceptions;
import org.teavm.jso.JSObject;
import org.teavm.jso.ajax.XMLHttpRequest;
import org.teavm.jso.browser.Window;

import areca.common.Platform;
import areca.common.Promise;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.testrunner.Skip;
import areca.common.testrunner.Test;

/**
 *
 * @author Falko Bräutigam
 */
@Test
public class TeavmRuntimeTest implements JSObject {

    private static final Log LOG = LogFactory.getLog( TeavmRuntimeTest.class );

    public static TeavmRuntimeTestClassInfo info = TeavmRuntimeTestClassInfo.instance();

    @JSBody(params = {"obj"}, script = "console.log( obj );")
    public static native void console( JSObject obj );


    @Test
    public Promise<?> idleCallbackTest() {
        return Platform.requestIdleCallback( deadline -> {
            LOG.info( "Deadline: remaining = %s", deadline.timeRemaining() );
        });
    }


    @Test
    @Skip
    public void stackTraceTest() {
        try {
//            String s = null;
//            s.toString();

            var a = new String[0];
            LOG.info( "a: length=%s", a.length );
            a[10] = "1";
            LOG.info( "a: length=%s", a.length );
            LOG.info( "a: %s", a[10] );
        }
        catch (Exception e) {
            var js = JSExceptions.getJSException( e );
            console( js );
        }
        var e = new RuntimeException( "Test" );
        e.fillInStackTrace();
        var js = JSExceptions.getJSException( e );
        Window.alert( js );
    }


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
            LOG.info( "Thread: " + Thread.currentThread() );
            new Thread( () -> {
                LOG.info( "Thread: " + Thread.currentThread() );
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
