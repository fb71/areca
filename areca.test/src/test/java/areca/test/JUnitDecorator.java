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
package areca.test;

import java.util.logging.Logger;

import areca.common.testrunner.TestRunner.TestMethod;
import areca.common.testrunner.TestRunner.TestResult;
import areca.common.testrunner.TestRunnerDecorator;

/**
 *
 * @author Falko Bräutigam
 */
public class JUnitDecorator
        extends TestRunnerDecorator {

    private static final Logger LOG = Logger.getLogger( JUnitDecorator.class.getSimpleName() );

    public boolean failed;

    @Override
    public void postTestMethod( TestMethod m, TestResult testResult ) {
        if (testResult.getException() != null) {
            failed = true;
        }
    }

}
