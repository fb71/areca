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
package areca.app.service.carddav;

import areca.common.Assert;
import areca.common.Platform;
import areca.common.Platform.HttpResponse;
import areca.common.Promise;
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
public class CardDavTest {

    private static final Log LOG = LogFactory.getLog( CardDavTest.class );

    public static final ClassInfo<CardDavTest> info = CardDavTestClassInfo.instance();

    @Test
    public Promise<HttpResponse> xhrTest() {
        return Platform.xhr( "GET", "http?uri=https://138.201.31.201:8443/" )
                .onReadyStateChange( state -> LOG.info( "ReadyState: " + state ) )
                .submit()
                .onSuccess( response -> {
                    LOG.info( "Status: %s", response.status() );
                    Assert.that( response.status() < 299, "Wrong status: " + response.status() );
                    System.out.println( response.text() );
                });
    }
}
