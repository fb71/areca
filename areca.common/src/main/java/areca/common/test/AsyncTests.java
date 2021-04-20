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

import areca.common.AsyncJob;
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

    private static final Log log = LogFactory.getLog( AsyncTests.class );

    public static final AsyncTestsClassInfo info = AsyncTestsClassInfo.instance();

    @Test
    public void asyncJobTest() throws Exception {
        var monitor = new Object();
        new AsyncJob()
                .schedule( "work 1", site -> {
                    log.info( "work 1" );
                })
                .schedule( "work 2", site -> {
                    log.info( "work 2" );
                    synchronized (monitor) {
                        monitor.notifyAll();
                    }
                })
                .start();

        synchronized (monitor) {
            monitor.wait( 1000 );
        }
    }

}
