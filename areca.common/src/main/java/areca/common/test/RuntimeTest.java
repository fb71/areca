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
package areca.common.test;

import java.lang.reflect.InvocationTargetException;

import areca.common.Assert;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.testrunner.Test;

/**
 *
 * @author Falko Bräutigam
 */
@Test
public class RuntimeTest {

    private static final Log log = LogFactory.getLog( RuntimeTest.class );

    public static final RuntimeTestClassInfo    info = RuntimeTestClassInfo.instance();

    protected boolean closed;

    @Test
    public void exceptionCauseTest() {
        Exception cause = new Exception();
        InvocationTargetException e = new InvocationTargetException( cause );
        Assert.isSame( cause, e.getCause() );
    }


    @Test(expected = UnsupportedOperationException.class)
    public void tryWithResourceTest() {
        try (AutoClose autoClose = new AutoClose()) {
            if (!closed) {
                throw new UnsupportedOperationException();
            }
        }
        Assert.that( closed );
    }


    class AutoClose implements AutoCloseable {
        @Override
        public void close() {
            closed = true;
            //throw new RuntimeException( "not yet implemented." );
        }
    }

}
