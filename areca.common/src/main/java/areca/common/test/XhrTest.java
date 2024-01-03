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
package areca.common.test;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;

import areca.common.Assert;
import areca.common.Platform;
import areca.common.Promise;
import areca.common.Promise.CancelledException;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.common.testrunner.Test;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class XhrTest {

    private static final Log LOG = LogFactory.getLog( XhrTest.class );

    public static final ClassInfo<XhrTest> info = XhrTestClassInfo.instance();

    public static final String URL_GET = "https://www.google.de";

    @Test
    public Promise<?> simpleGetTest() {
        return Platform.xhr( "GET", URL_GET )
                .submit()
                .onSuccess( response -> {
                    Assert.isEqual( 200, response.status() );
                    LOG.warn( StringUtils.abbreviate( response.text(), 256 ) );
                });
    }

    @Test
    public void cancelTest() {
        var flag = new AtomicBoolean();
        var result = Platform.xhr( "GET", URL_GET )
                .submit()
                .onSuccess( response -> {
                    Assert.fail( "This must not be reached." );
                })
                .onError( e -> {
                    Assert.isType( CancelledException.class, e, "Wrong exception type" );
                    flag.set( true );
                });
        result.cancel();
        result.waitForResult();
        Assert.that( flag.get(), "onError() was not reached." );
    }

}
