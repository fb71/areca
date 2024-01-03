package areca.rt.server.test;
/*
 * Copyright (C) 2023, the @authors. All rights reserved.
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


import org.junit.jupiter.api.Test;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * JUnit test runner for Areca core tests, running inside the JVM.
 * <p>
 * Just a single test for better debugging.
 *
 * @author Falko Bräutigam
 */
class JUnitSingleTest extends JUnitTestBase {

    private static final Log LOG = LogFactory.getLog( JUnitSingleTest.class );

    @Test
    public void simpleTest() {
    }

}
