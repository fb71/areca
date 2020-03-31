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
package areca.common.testrunner;

import java.util.logging.Logger;

import areca.common.reflect.RuntimeInfo;
import areca.common.testrunner.TestRunner.Decorator;
import areca.common.testrunner.TestRunner.TestMethod;


/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class LogDecorator
        extends Decorator {

    private static final Logger LOG = Logger.getLogger( LogDecorator.class.getSimpleName() );

    @Override
    public void preRun( TestRunner runner ) {
        LOG.info( "Running tests..." );
    }

    @Override
    public void postRun( TestRunner runner ) {
        LOG.info( "Tests done." );
    }

    @Override
    public void preTest( Object test ) {
        LOG.info( "===< " + test.getClass().getName() + " >=========" );
    }

    @Override
    public void preTestMethod( TestMethod m ) {
        LOG.info( "---< " + m.name() + "() >---------" );
    }

    @Override
    public void postTestMethod( TestMethod m ) {
    }

}
