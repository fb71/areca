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
package areca.aws.ec2proxy;

import java.util.function.Predicate;

import java.net.http.HttpClient;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.concurrent.LazyInitializer;

import areca.aws.AWS;
import areca.aws.Lazy;
import areca.aws.ec2proxy.VHost.ProxyPath;

/**
 *
 * @author Falko Bräutigam
 */
public abstract class RequestHandler {

    protected Predicate<Probe> predicate;

    protected RequestHandler( Predicate<Probe> predicate ) {
        this.predicate = predicate;
    }

    public boolean canHandle( Probe probe ) {
        return predicate.test( probe );
    }

    public abstract void handle( Probe probe ) throws Exception;

    /**
     *
     */
    public static class Probe {

        public Probe( HttpServletRequest request, HttpServletResponse response ) {
            this.request = request;
            this.response = response;
        }

        public HttpClient http;

        public VHost vhost;

        //public String redirect;

        public ProxyPath proxyPath;

        public HttpServletRequest request;

        public HttpServletResponse response;

        public Exception error;

        public AWS aws;

        /**
         * Cache {@link #request} content for long running operations.
         * Especially github webhooks which seem to close request quickly.
         */
        public LazyInitializer<byte[]> requestBody = Lazy.of( () -> IOUtils.toByteArray( request.getInputStream() ) );

    }

}
