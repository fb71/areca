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
import areca.common.testrunner.Before;
import areca.common.testrunner.Skip;
import areca.common.testrunner.Test;
import areca.rt.teavm.SetTimeoutEventManager;

/**
 *
 * @author Falko Bräutigam
 */
@Test
public class SetTimeoutEventManagerTest
        extends EventManagerTest{

    @SuppressWarnings("hiding")
    public static final ClassInfo<SetTimeoutEventManagerTest> info = SetTimeoutEventManagerTestClassInfo.instance();

    @Before
    protected void setup() {
        this.em = new SetTimeoutEventManager();
        super.setup();
    }

    @Test @Skip
    @Override
    public void test() {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

}
