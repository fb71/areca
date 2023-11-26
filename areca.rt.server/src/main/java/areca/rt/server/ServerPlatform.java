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
package areca.rt.server;

import java.util.concurrent.Callable;

import areca.common.Platform;
import areca.common.Platform.HttpRequest;
import areca.common.Platform.IdleDeadline;
import areca.common.Promise;
import areca.common.Promise.Completable;
import areca.common.Session;
import areca.common.base.Consumer.RConsumer;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public class ServerPlatform
        implements Platform.PlatformImpl {

    private static final Log LOG = LogFactory.getLog( ServerPlatform.class );

    @Override
    public <R> Promise<R> schedule( int delayMillis, Callable<R> task ) {
        Completable<R> promise = new Completable<>();
        Session.instanceOf( EventLoop.class ).enqueue( "schedule", () -> {
            try {
                if (!promise.isCanceled()) {
                    promise.complete( task.call() );
                }
            }
            catch (Throwable e) {
                promise.completeWithError( e );
            }
        }, delayMillis );
        return promise;
    }

    @Override
    public Promise<Void> requestAnimationFrame( RConsumer<Double> callback ) {
        Completable<Void> promise = new Completable<>();
        Session.instanceOf( EventLoop.class ).enqueue( "animation frame", () -> {
            try {
                if (!promise.isCanceled()) {
                    callback.accept( (double)System.currentTimeMillis() );
                    promise.complete( null );
                }
            }
            catch (Throwable e) {
                promise.completeWithError( e );
            }
        }, 0 );
        return promise;
    }

    @Override
    public Promise<Void> requestIdleCallback( RConsumer<IdleDeadline> callback ) {
        Completable<Void> promise = new Completable<>();
        Session.instanceOf( EventLoop.class ).enqueue( "idle callback", () -> {
            try {
                if (!promise.isCanceled()) {
                    var deadline = new IdleDeadline() {
                        @Override public double timeRemaining() {
                            return 100;
                        }
                    };
                    callback.accept( deadline );
                    promise.complete( null );
                }
            }
            catch (Throwable e) {
                promise.completeWithError( e );
            }
        }, 0 );
        return promise;
    }

    @Override
    public HttpRequest xhr( String method, String url ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

}
