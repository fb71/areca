/*
 * Copyright (C) 2022, the @authors. All rights reserved.
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
package areca.app;

import areca.common.Promise;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.RuntimeInfo;
import areca.common.test.AsyncEventManagerTest;
import areca.common.test.SameStackEventManagerTest;
import areca.common.testrunner.Test;

/**
 * Single test methods from other test to run in (sync) benchmark mode.
 * 
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class BenchTest {

    private static final Log LOG = LogFactory.getLog( BenchTest.class );

    public static final BenchTestClassInfo info = BenchTestClassInfo.instance();

    @Test
    public Promise<?> asyncEventManagerPerformance() {
        var test = new AsyncEventManagerTest() {{setup();}};
        return test.performanceTest();
    }

    @Test
    public Promise<?> syncEventManagerPerformance() {
        var test = new SameStackEventManagerTest() {{setup();}};
        return test.performanceTest();
    }
}
