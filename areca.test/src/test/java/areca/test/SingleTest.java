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
package areca.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.polymap.model2.engine.EntityRepositoryImpl;
import org.polymap.model2.test2.PerformanceTest;
import org.polymap.model2.test2.RepoSupplier;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Level;
import areca.common.log.LogFactory.Log;

/**
 * JUnit test runner for Areca core tests, running inside the JVM.
 * <p>
 * Just a single test for better debugging.
 *
 * @author Falko Bräutigam
 */
class SingleTest
        extends JUnitTestBase {

    private static final Log LOG = LogFactory.getLog( SingleTest.class );

    @BeforeAll
    protected static void setupLogging() {
        // suppress "Entity class is already connected..." warning
        LogFactory.setClassLevel( EntityRepositoryImpl.class, Level.ERROR );

        //LogFactory.setPackageLevel( No2Store.class, Level.DEBUG );
        //LogFactory.setPackageLevel( AssociationsTest.class, Level.DEBUG );
        RepoSupplier.no2();
    }

    @Test
    public void theOnlyTest() {
        execute( PerformanceTest.info );
        //execute( SimpleQueryTest.info );
    }

}
