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

import areca.common.base.Sequence;
import areca.common.base.Consumer.RConsumer;

/**
 *
 * @author Falko Bräutigam
 */
public abstract class Platform {

    public static PlatformImpl impl;


    public static <R> Promise<R> schedule( int delayMillis, Callable<R> task ) {
        return impl.schedule( delayMillis, task );
    }


    public static void schedule( int delayMillis, Runnable task ) {
        schedule( delayMillis, () -> {
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
     * Prepares a XMLHttpRequest.
     */
    public static HttpRequest xhr( String method, String url ) {
        return impl.xhr( method, url );
    }


    /**
     *
     */
    public interface PlatformImpl {

        public <R> Promise<R> schedule( int delayMillis, Callable<R> task );

        public HttpRequest xhr( String method, String url );
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

        protected abstract HttpRequest authenticate( String username, String password );

        protected abstract Promise<HttpResponse> doSubmit( Object jsonOrStringData );

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

        public abstract String text();

        public abstract <R /*extends JSObject*/> R json();

        public abstract /*Document*/ Object xml();
    }
}
