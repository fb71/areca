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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSMethod;
import org.teavm.jso.JSObject;
import org.teavm.jso.ajax.XMLHttpRequest;
import org.teavm.jso.browser.Window;

import areca.common.Assert;
import areca.common.Platform;
import areca.common.Platform.HttpRequest;
import areca.common.Platform.HttpResponse;
import areca.common.Platform.IdleDeadline;
import areca.common.Promise;
import areca.common.Promise.Completable;
import areca.common.Timer;
import areca.common.base.Consumer.RConsumer;
import areca.common.base.Opt;
import areca.common.base.Supplier.RSupplier;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public class TeaPlatform
        implements Platform.PlatformImpl, JSObject {

    private static final Log LOG = LogFactory.getLog( TeaPlatform.class );


    @Override
    public Promise<Void> requestAnimationFrame( RConsumer<Double> callback ) {
        return new Completable<Void>() {

            int id = Window.requestAnimationFrame( timestamp -> {
                Assert.that( !isCanceled() );
                callback.accept( timestamp );
                complete( null );
            });

            @Override
            public void cancel() {
                Window.cancelAnimationFrame( id );
                super.cancel();
            }
        };
    }


    @Override
    public Promise<Void> requestIdleCallback( RConsumer<Platform.IdleDeadline> callback ) {
        return new Completable<Void>() {

            int id = _requestIdleCallback( deadline -> {
                Assert.that( !isCanceled() );
                callback.accept( new IdleDeadline() {
                    @Override public double timeRemaining() { return deadline.timeRemaining(); }
                });
                complete( null );
            });

            @Override
            public void cancel() {
                _cancelIdleCallback( id );
                super.cancel();
            }
        };
    }

    @JSBody(params = "callback", script = "return window.requestIdleCallback(callback);")
    protected static native int _requestIdleCallback( _IdleCallback callback );

    @JSBody(params = "id", script = "return window.cancelIdleCallback(id);")
    protected static native void _cancelIdleCallback( int id );

    @JSFunctor
    protected interface _IdleCallback extends JSObject {
        void onIdle( _IdleDeadline deadline );
    }

    public abstract class _IdleDeadline implements JSObject {
        @JSMethod()
        public abstract double timeRemaining();
    }


    @Override
    public <R> Promise<R> schedule( int delayMillis, Callable<R> task ) {
        return new Completable<R>() {
            private int id = Window.setTimeout( () -> {
                try {
                    Assert.that( !isCanceled() );
                    complete( task.call() );
                }
                catch (Throwable e) {
                    completeWithError( e );
                }
            }, delayMillis );

            @Override
            public void cancel() {
                Window.clearTimeout( id );
                super.cancel();
            }
        };
    }


    @Override
    public void waitForCondition( RSupplier<Boolean> condition, Object target ) {
        if (!condition.get()) {
            synchronized (target) {
                while (!condition.get()) {
                    try {
                        target.wait();
                    }
                    catch (InterruptedException e) {
                    }
                }
            }
        }
    }


    @Override
    public HttpRequest xhr( String method, String url ) {
        return new HttpRequest() {
            private XMLHttpRequest request = XMLHttpRequest.create();
            private Opt<String> username = Opt.absent();
            private Opt<String> password = Opt.absent();
            private Map<String,String> headers = new HashMap<>();
            private Opt<String> overrideMimeType = Opt.absent();
            private Timer timer;

            @Override
            public HttpRequest onReadyStateChange( RConsumer<ReadyState> handler ) {
                request.setOnReadyStateChange( () -> {
                    handler.accept( ReadyState.valueOf( request.getReadyState() ) );
                });
                return this;
            }

            @Override
            @SuppressWarnings("hiding")
            public HttpRequest authenticate( String username, String password ) {
                this.username = Opt.of( username );
                this.password = Opt.of( password );
                return this;
            }

            @Override
            public HttpRequest addHeader( String name, String value ) {
                if (headers.put( name, value ) != null) {
                    throw new UnsupportedOperationException( "Multiple values for name: " + name );
                }
                return this;
            }

            @Override
            public HttpRequest overrideMimeType( String mimeType ) {
                overrideMimeType = Opt.of( mimeType );
                return this;
            }

            @Override
            protected Promise<HttpResponse> doSubmit( Object jsonOrStringData ) {
                var promise = new Promise.Completable<HttpResponse>() {
                    @Override public void cancel() {
                        request.abort();
                        super.cancel();
                    }
                };
                request.open( method, url, true ); //, username, password );
                username.ifPresent( v -> request.setRequestHeader( "X-auth-username", v ) );
                password.ifPresent( v -> request.setRequestHeader( "X-auth-password", v ) );
                headers.forEach( (n,v) -> request.setRequestHeader( n, v ) );
                overrideMimeType.ifPresent( mimeType -> request.overrideMimeType( mimeType ) );
                request.onComplete( () -> {
                    if (!promise.isCanceled()) {
                        promise.complete( new HttpResponse() {
                            @Override
                            public int status() {
                                return request.getStatus();
                            }
                            @Override
                            public Object content() {
                                return request.getResponse();
                            }
                            @Override
                            public String text() {
                                return request.getResponseText();
                            }
                            @Override
                            @SuppressWarnings("unchecked")
                            public <R> R json() {
                                return (R)request.getResponse();
                            }
                            @Override
                            public Object xml() {
                                return request.getResponseXML();
                            }
                        });
                    }
                });
                timer = Timer.start();
                if (jsonOrStringData == null) {
                    request.send();
                }
                else if (jsonOrStringData instanceof String) {
                    request.send( (String)jsonOrStringData );
                }
                else { //if (jsonOrStringData instanceof JSObject) {
                    request.send( (JSObject)jsonOrStringData );
                }
                return promise;
            }
        };
    }

}
