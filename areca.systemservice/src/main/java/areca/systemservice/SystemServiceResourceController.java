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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.milton.annotations.ResourceController;
import io.milton.annotations.Root;

/**
 *
 * @author Falko Bräutigam
 */
@ResourceController
public class SystemServiceResourceController {

    static final Log log = LogFactory.getLog( SystemServiceResourceController.class );

    @Root
    public String getRoot() {
        return "root"; //this;
    }

    //@Name
    public String getName() {
       return "areca";
    }

//    @ChildrenOf
//    public List<EmailFolder> getEmailFolders( SystemServiceResourceController root ) {
//        log.info( "getEmailMessages(): ..." );
//        return Arrays.asList( new EmailFolder( "First" ) );
//    }
//
//
//    @ChildrenOf
//    public List<EmailMessage> getEmailMessages( EmailFolder folder ) {
//        log.info( "getEmailMessages(): " + folder.getName() );
//        return Arrays.asList( new EmailMessage( "Message!" ) );
//    }

}
