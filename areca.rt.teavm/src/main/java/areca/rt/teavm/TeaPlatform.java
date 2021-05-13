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
package areca.rt.teavm;

import java.util.concurrent.Callable;

import org.teavm.jso.browser.Window;

import areca.common.Platform;
import areca.common.Promise;
import areca.common.Promise.Completable;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public class TeaPlatform
        extends Platform {

    private static final Log LOG = LogFactory.getLog( TeaPlatform.class );

    @Override
    public <R> Promise<R> schedule( int delayMillis, Callable<R> task ) {
        Completable<R> promise = new Completable<>();
        Window.setTimeout( () -> {
            try {
                promise.complete( task.call() );
            }
            catch (Exception e) {
                promise.completeWithError( e );
            }
        }, delayMillis );
        return promise;
    }

}
