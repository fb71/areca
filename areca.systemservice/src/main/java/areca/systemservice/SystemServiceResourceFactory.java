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

import java.util.Arrays;
import java.util.List;
import javax.mail.MessagingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import areca.systemservice.email.AccountFolderResource;
import io.milton.common.Path;
import io.milton.http.ResourceFactory;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.Resource;

/**
 *
 * @author Falko Bräutigam
 */
public class SystemServiceResourceFactory
        implements ResourceFactory {

    private static final Log log = LogFactory.getLog( SystemServiceResourceFactory.class );

    private FolderResourceBase              root;

    private List<AccountFolderResource>     accounts = Arrays.asList(
            new AccountFolderResource( "mail.polymap.de", "areca@polymap.de", "dienstag" )
    );


    public SystemServiceResourceFactory() throws MessagingException {
        root = new FolderResourceBase() {
            @Override
            protected Iterable<AccountFolderResource> createChildren() throws Exception {
                return accounts;
            }
            @Override
            public String getName() {
                return "root";
            }
        };
    }


    @Override
    public Resource getResource( String host, String pathString ) throws NotAuthorizedException, BadRequestException {
        Path path = Path.path( pathString );

        // XXX strip contextPath
        path = path.getStripFirst().getStripFirst();
        log.debug( "getResource(): '" + path + "'" );

        FolderResourceBase cursor = root;
        for (; path.getLength() > 0; path = path.getStripFirst()) {
            String name = path.getFirst();
            log.debug( "    name=" + name + ", parent=" + cursor.getClass().getSimpleName() + ", path=" + path );
            Resource child = cursor.child( name );
            if (child instanceof FolderResourceBase) {
                cursor = (FolderResourceBase)child;
            }
            else {
                if (path.getLength() > 1) {
                    throw new IllegalStateException( "Found non-folder for path: " + path );
                }
                return child;
            }
        }
        log.debug( "getResource(): " + cursor.getName() );
        return cursor;
    }

}
