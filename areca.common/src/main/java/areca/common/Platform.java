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
package areca.common;

import java.util.concurrent.Callable;

import areca.common.base.Consumer.RConsumer;
import areca.common.base.Sequence;
import areca.common.base.Supplier.RSupplier;

/**
 *
 * @author Falko Bräutigam
 */
public abstract class Platform {

    public static PlatformImpl impl;

    public static Scheduler scheduler = new Scheduler();

    /**
     * Returns true if the current Platform runs in the JVM.
     * Returns false if the current Platform runs in the Browser/JavaScript.
     */
    public static boolean isJVM() {
        return Assert.notNull( impl, "Platform not (yet) initialized." )
                .getClass().getSimpleName().contains( "Server" );
    }


    public static <R> Promise<R> schedule( int delayMillis, Callable<R> task ) {
        return impl.schedule( delayMillis, task );
    }


    public static Promise<?> schedule( int delayMillis, Runnable task ) {
        return schedule( delayMillis, () -> {
            task.run();
            return null;
        });
    }


    public static void async( Runnable task ) {
        schedule( 0, task );
    }


    public static <R> Promise<R> async( Callable<R> task ) {
        return schedule( 0, task );
    }


    public static Throwable rootCause( Throwable e ) {
        Throwable cause = e;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }


    /**
     *
     * @param callback Callback handling the timestamp of the call.
     * @return Identifier of this request used to cancel the request.
     */
    public static Promise<Void> requestAnimationFrame( RConsumer<Double> callback ) {
        return impl.requestAnimationFrame( callback );
    }


    /**
     *
     *
     * @param callback
     */
    public static Promise<Void> requestIdleCallback( RConsumer<IdleDeadline> callback ) {
        return impl.requestIdleCallback( callback );
    }

    public interface IdleDeadline {
        public double timeRemaining();
    }


    /**
     * Prepares a XMLHttpRequest.
     */
    public static HttpRequest xhr( String method, String url ) {
        return impl.xhr( method, url );
    }


    protected static void waitForCondition( RSupplier<Boolean> condition, Object target ) {
        impl.waitForCondition( condition, target );
    }

    /**
     *
     */
    public interface PlatformImpl {

        public <R> Promise<R> schedule( int delayMillis, Callable<R> task );

        public void waitForCondition( RSupplier<Boolean> condition, Object target );

        public HttpRequest xhr( String method, String url );

        public Promise<Void> requestAnimationFrame( RConsumer<Double> callback );

        public Promise<Void> requestIdleCallback( RConsumer<IdleDeadline> callback );
    }


    /**
     * XMLHttpRequest
     */
    public static abstract class HttpRequest {

        public enum ReadyState {
            UNSET, OPENED, HEADERS_RECEIVED, LOADING, DONE;

            public static ReadyState valueOf( int ordinal ) {
                return Sequence.of( values() ).first( v -> v.ordinal() == ordinal ).orElseError();
            }
        }

        public abstract HttpRequest onReadyStateChange( RConsumer<ReadyState> handler );

        public abstract HttpRequest authenticate( String username, String password );

        public abstract HttpRequest addHeader( String name, String value );

        public abstract HttpRequest overrideMimeType( String mimeType );

        protected abstract Promise<HttpResponse> doSubmit( Object jsonOrStringData );

        /**
         *
         * @see Promise#priority(areca.common.Scheduler.Priority)
         */
        public Promise<HttpResponse> submit() {
            return doSubmit( null );
        }

        public Promise<HttpResponse> submit( String data ) {
            return doSubmit( data );
        }
    }

    /**
     *
     */
    public static abstract class HttpResponse {

        public abstract int status();

        public abstract /*JSObject*/ Object content();

        public abstract String text();

        public abstract <R /*extends JSObject*/> R json();

        public abstract /*Document*/ Object xml();
    }

    /**
     *
     */
    public static class HttpServerException
            extends Exception {

        public String   message;

        public int      httpStatus;

        public String   responseBody;

        public HttpServerException( int httpStatus, String responseBody ) {
            switch (httpStatus) {
                case 401: message = "Authentication failed. Wrong username and/or password."; break;
                case 500: message = "Internal server error: " + httpStatus; break;
                default: message = "Request failed with HTTP status: " + httpStatus;
            }
            this.httpStatus = httpStatus;
            this.responseBody = responseBody;
        }

        @Override
        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return String.format( "%s: %s", getClass().getSimpleName(), message );
        }
    }
}
