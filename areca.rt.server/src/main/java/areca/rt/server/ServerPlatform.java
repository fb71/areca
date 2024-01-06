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
import java.util.concurrent.CancellationException;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

import areca.common.Platform;
import areca.common.Platform.HttpRequest;
import areca.common.Platform.HttpResponse;
import areca.common.Platform.IdleDeadline;
import areca.common.Promise;
import areca.common.Promise.Completable;
import areca.common.Session;
import areca.common.base.Consumer.RConsumer;
import areca.common.base.Supplier.RSupplier;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public class ServerPlatform
        implements Platform.PlatformImpl {

    private static final Log LOG = LogFactory.getLog( ServerPlatform.class );

    public static final Duration HTTP_TIMEOUT = Duration.ofSeconds( 30 );

    // instance *******************************************

    private HttpClient      http = HttpClient.newBuilder().connectTimeout( HTTP_TIMEOUT ).build();


    @Override
    public void waitForCondition( RSupplier<Boolean> condition, Object target ) {
        var eventLoop = Session.instanceOf( EventLoop.class );

        eventLoop.execute();
        while( !condition.get() ) {
            try {
                Thread.sleep( eventLoop.pendingWait() );
            }
            catch (InterruptedException e) { }
            eventLoop.execute();
        }
    }


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
        try {
//            if (!url.startsWith( "http:" ) && !url.startsWith( "https:" )) {
//                url = ServletContext...
//            }
            return new HttpRequest() {

                private Builder b = java.net.http.HttpRequest.newBuilder()
                        .uri( new URI( url ) )
                        .timeout( HTTP_TIMEOUT );

                @Override
                public HttpRequest onReadyStateChange( RConsumer<ReadyState> handler ) {
                    // XXX Auto-generated method stub
                    throw new RuntimeException( "not yet implemented." );
                }

                @Override
                public HttpRequest authenticate( String username, String password ) {
                    // XXX Auto-generated method stub
                    throw new RuntimeException( "not yet implemented." );
                }

                @Override
                public HttpRequest addHeader( String name, String value ) {
                    b.header( name, value );
                    return this;
                }

                @Override
                public HttpRequest overrideMimeType( String mimeType ) {
                    // XXX Auto-generated method stub
                    throw new RuntimeException( "not yet implemented." );
                }

                @Override
                protected Promise<HttpResponse> doSubmit( Object jsonOrStringData ) {
                    b = jsonOrStringData == null
                        ? b.method( method, BodyPublishers.noBody() )
                        : b.method( method, BodyPublishers.ofString( (String)jsonOrStringData ) );

                    var request = b.build();
                    var future = http.sendAsync( request, BodyHandlers.ofString() );

                    var promise = new Promise.Completable<HttpResponse>() {
                        @Override public void cancel() {
                            future.cancel( true );
                            super.cancel();
                        }
                    };

                    var eventLoop = Session.instanceOf( EventLoop.class );
                    eventLoop.requestPolling();
                    future.whenComplete( (response,e) -> {
                        LOG.warn( "XHR: whenComplete(): ...");
                        eventLoop.releasePolling( "xhr", () -> {
                            LOG.warn( "XHR: enqueued(): ...");
                            if (e != null) {
                                if (!(e instanceof CancellationException)) {
                                    promise.completeWithError( e );
                                }
                            }
                            else {
                                promise.complete( new HttpResponse() {
                                    @Override
                                    public int status() {
                                        return response.statusCode();
                                    }
                                    @Override
                                    public Object content() {
                                        // XXX Auto-generated method stub
                                        throw new RuntimeException( "not yet implemented." );
                                    }
                                    @Override
                                    public String text() {
                                        return response.body();
                                    }
                                    @Override
                                    public <R> R json() {
                                        // XXX Auto-generated method stub
                                        throw new RuntimeException( "not yet implemented." );
                                    }
                                    @Override
                                    public Object xml() {
                                        // XXX Auto-generated method stub
                                        throw new RuntimeException( "not yet implemented." );
                                    }
                                } );
                            }
                        }, 0);
                    });
                    return promise;
                }
            };
        }
        catch (URISyntaxException e) {
            throw new RuntimeException( e );
        }
    }

}
