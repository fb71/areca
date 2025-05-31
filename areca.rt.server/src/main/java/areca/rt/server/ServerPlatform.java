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

import java.util.concurrent.CancellationException;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

import areca.common.Assert;
import areca.common.Platform;
import areca.common.Platform.HttpRequest;
import areca.common.Platform.HttpResponse;
import areca.common.Platform.IdleDeadline;
import areca.common.Platform.PollingCommand;
import areca.common.Promise;
import areca.common.Promise.Completable;
import areca.common.Session;
import areca.common.Timer;
import areca.common.base.Consumer;
import areca.common.base.Consumer.RConsumer;
import areca.common.base.Lazy.RLazy;
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

    /** One HttpClient for all {@link Session}s */
    private RLazy<HttpClient>   http = new RLazy<>( () -> HttpClient.newBuilder().connectTimeout( HTTP_TIMEOUT ).build() );


    @Override
    public void dispose() {
        if (http != null) {
            http = null; // XXX no close()/stop() API!?? :(
        }
    }


    @Override
    public void waitForCondition( RSupplier<Boolean> condition, Object target ) {
        var eventloop = Session.instanceOf( EventLoop.class );
        var met = eventloop.execute( condition );
        Assert.that( met );
    }


    @Override
    public void polling( PollingCommand cmd ) {
        switch (cmd) {
            case START : Session.instanceOf( EventLoop.class ).requestPolling(); break;
            case STOP : Session.instanceOf( EventLoop.class ).releasePolling( ); break;
        }
    }


    @Override
    public <R> Promise<R> enqueue( String label, int delayMillis, Consumer<Completable<R>,Exception> task ) {
        Assert.that( delayMillis >= 0 );
        var t = delayMillis > 0 ? Timer.start() : null;
        var promise = new Completable<R>();
//        var callerStack = new Exception( "Caller stack" ).fillInStackTrace();
        Session.instanceOf( EventLoop.class ).enqueue( label, () -> {
            if (t != null) {
                LOG.debug( "enqueue(): delay requested: %s - was actually: %s", delayMillis, t.elapsedHumanReadable() );
            }
            try {
                if (!promise.isCanceled()) { // XXX
                    task.accept( promise );
                }
            }
            catch (Throwable e) {
//                callerStack.initCause( e );
//                promise.completeWithError( callerStack );
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
            // check internal/relative URLs on server
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
                    var future = http.get().sendAsync( request, BodyHandlers.ofString() );

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
