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
import org.polymap.model2.store.no2.No2Store;
import org.polymap.model2.test2.AssociationsTest;
import org.polymap.model2.test2.ComplexModelTest;
import org.polymap.model2.test2.RepoSupplier;
import org.polymap.model2.test2.RuntimeTest;
import org.polymap.model2.test2.SimpleModelTest;
import org.polymap.model2.test2.SimpleQueryTest;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Level;
import areca.common.log.LogFactory.Log;

/**
 * Model2 Tests with Nitrite/No2 backend.
 *
 * @author Falko Bräutigam
 */
public class Model2Tests extends JUnitTestBase {

    private static final Log LOG = LogFactory.getLog( Model2Tests.class );

    @BeforeAll
    protected static void setupLogging() {
        // suppress "Entity class is already connected..." warning
        LogFactory.setClassLevel( EntityRepositoryImpl.class, Level.ERROR );
        LogFactory.setPackageLevel( No2Store.class, Level.INFO );
        RepoSupplier.no2();
    }

    @Test
    public void runtimeTests() {
        execute( RuntimeTest.info );
    }

    @Test
    public void simpleModelTests() {
        execute( SimpleModelTest.info );
    }

    @Test
    public void simpleQueryTests() {
        execute( SimpleQueryTest.info );
    }

    @Test
    public void complexModelTests() {
        execute( ComplexModelTest.info );
    }

    @Test
    public void associationsTests() {
        execute( AssociationsTest.info );
    }

}
