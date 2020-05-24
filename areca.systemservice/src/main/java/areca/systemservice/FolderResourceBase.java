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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.mail.Folder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;

/**
 *
 * @author Falko Bräutigam
 */
public abstract class FolderResourceBase
        extends ResourceBase
        implements CollectionResource {

    private static final Log log = LogFactory.getLog( FolderResourceBase.class );

    private Map<String,Resource>            childrenCache;


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

}
