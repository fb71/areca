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

import java.util.Date;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.milton.http.Auth;
import io.milton.http.Request;
import io.milton.http.Request.Method;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.PropFindableResource;

/**
 *
 * @author Falko Bräutigam
 */
public abstract class ResourceBase
        implements PropFindableResource {

    private static final Log log = LogFactory.getLog( ResourceBase.class );

    protected ResourceBase          parent;


    @SuppressWarnings("hiding")
    void init( ResourceBase parent ) {
        this.parent = parent;
    }


    @SuppressWarnings("unchecked")
    protected <R> R parent( Class<R> type ) {
        ResourceBase candidate = parent;
        while (!type.equals( candidate.getClass() )) {
            candidate = candidate.parent;
        }
        return (R)candidate;
    }

    // abstract no-op implementations *********************

    @Override
    public String getUniqueId() {
        return null;
    }

    @Override
    public Date getModifiedDate() {
        return null;
    }

    @Override
    public Object authenticate( String user, String password ) {
        return "ok";
    }

    @Override
    public boolean authorise( Request request, Method method, Auth auth ) {
        return true;
    }

    @Override
    public String getRealm() {
        return getName();
    }

    @Override
    public String checkRedirect( Request request ) throws NotAuthorizedException, BadRequestException {
        return null;
    }


    @Override
    public Date getCreateDate() {
        return null;
    }

}
