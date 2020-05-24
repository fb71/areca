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

import java.util.Arrays;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import areca.common.base.Sequence;
import areca.systemservice.FolderResourceBase;

/**
 *
 * @author Falko Bräutigam
 */
public class AccountFolderResource
        extends FolderResourceBase {

    private static final Log log = LogFactory.getLog( AccountFolderResource.class );

    private String      server;

    private String      username;

    private String      password;

    private Folder      root;

    private Store       store;


    public AccountFolderResource( String server, String username, String password ) throws MessagingException {
        this.server = server;
        this.username = username;
        this.password = password;
    }


    @Override
    public String getName() {
        return username;  // + "@" + server;
    }


    @Override
    protected Iterable<EmailFolderResource> createChildren() throws Exception {
        log.info( "Connecting to IMAP server: " + server );
        Properties props = System.getProperties();
        props.setProperty( "mail.store.protocol", "imaps" );
        props.setProperty( "mail.imaps.ssl.trust", "*" );
        Session session = Session.getDefaultInstance( props, null );
        store = session.getStore( "imaps" );
        store.connect( server, username, password  );
        root = store.getDefaultFolder();

        log.info( "Fetching folders of: " + getName() + "..." );
        //folders = Arrays.asList( root.list( "*" ) );
        //log.debug( "    Folders: " + Sequence.of( folders ).transform( Folder::getFullName ) );
        return Sequence.of( childrenOf( root ) ).transform( f -> new EmailFolderResource( f ) ).asIterable();
    }


    /**
     * Direct child folders of the given Folder.
     */
    protected Iterable<Folder> childrenOf( Folder parentFolder ) throws MessagingException {
        return Arrays.asList( parentFolder.list( "%" ) );
//        return Sequence.of( folders )
//                .filter( f -> String.join( "/", parentFolder.getFullName(), f.getName() ).equals( f.getFullName() ) )
//                .asIterable();
    }


//    protected List<String> pathOf( Folder f ) {
//        return Arrays.asList( StringUtils.split( f.getFullName(), "/" ) );
//    }

}
