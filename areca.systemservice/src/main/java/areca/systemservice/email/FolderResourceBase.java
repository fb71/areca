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
package areca.systemservice.email;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.mail.Folder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.milton.http.Auth;
import io.milton.http.Request;
import io.milton.http.Request.Method;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.CollectionResource;
import io.milton.resource.PropFindableResource;
import io.milton.resource.Resource;

/**
 *
 * @author Falko Bräutigam
 */
public abstract class FolderResourceBase
        implements CollectionResource, PropFindableResource {

    private static final Log log = LogFactory.getLog( FolderResourceBase.class );

    protected FolderResourceBase            parent;

    private Map<String,Resource>            childrenCache;


    @SuppressWarnings("hiding")
    void init( FolderResourceBase parent ) {
        this.parent = parent;
    }


    @SuppressWarnings("unchecked")
    protected <R> R parent( Class<R> type ) {
        FolderResourceBase candidate = parent;
        while (!type.equals( candidate.getClass() )) {
            candidate = candidate.parent;
        }
        return (R)candidate;
    }


    protected abstract Iterable<? extends Resource> createChildren() throws Exception;


    protected Map<String,? extends Resource> children() {
        if (childrenCache == null) {
            synchronized (this) {
                if (childrenCache == null) {
                    try {
                        log.info( "createChildren(): " + getName() );
                        childrenCache = new HashMap<>();
                        for (Resource child : createChildren()) {
                            if (child instanceof FolderResourceBase) {
                                ((FolderResourceBase)child).init( this );
                            }
                            childrenCache.put( child.getName(), child );
                        }
                    }
                    catch (RuntimeException e) {
                        throw e;
                    }
                    catch (Exception e) {
                        throw new RuntimeException( e );
                    }
                }
            }
        }
        return childrenCache;
    }


    @Override
    public Resource child( String childName ) throws NotAuthorizedException, BadRequestException {
        return children().get( childName );
    }


    @Override
    public List<Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        return new ArrayList<>( children().values() );
    }


    protected List<Folder> fetchFolders( Folder parentFolder ) {
        throw new UnsupportedOperationException( "not yet..." );
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
