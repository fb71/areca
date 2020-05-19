/*
 * Copyright (C) 2020, the @authors. All rights reserved.
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
package areca.systemservice;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.milton.http.AuthenticationHandler;
import io.milton.http.Request;
import io.milton.resource.Resource;

/**
 *
 * @author Falko Bräutigam
 */
public class NoAuthenticationHandler
        implements AuthenticationHandler {

    private static final Log log = LogFactory.getLog( NoAuthenticationHandler.class );

    @Override
    public boolean supports( Resource r, Request request ) {
        log.debug( "supports(): " + request );
        return true;
    }

    @Override
    public Object authenticate( Resource resource, Request request ) {
        log.debug( "autheticate(): " + request );
        return "yes!";
    }

    @Override
    public void appendChallenges( Resource resource, Request request, List<String> challenges ) {
        log.debug( "appendChallenges()" + request );
    }

    @Override
    public boolean isCompatible( Resource resource, Request request ) {
        log.debug( "isCompatible(): " + request );
        return true;
    }

    @Override
    public boolean credentialsPresent( Request request ) {
        log.debug( "credentialsPresent()" + request );
        return false;
    }

}
