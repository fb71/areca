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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import areca.systemservice.email.EmailFolder;
import areca.systemservice.email.EmailMessage;
import io.milton.annotations.ChildrenOf;
import io.milton.annotations.ResourceController;

/**
 *
 * @author Falko Bräutigam
 */
@ResourceController
public class EmailResourceController {

    private static final Log log = LogFactory.getLog( EmailResourceController.class );

    private List<EmailFolder>     folders = Arrays.asList( new EmailFolder( "First" ) );

    private List<EmailMessage>    messages = Arrays.asList( new EmailMessage( "Message" ) );

    @ChildrenOf
    public List<EmailFolder> getEmailFolders( String root ) {
        log.info( "getEmailFolders(): ..." );
        return folders;
    }


    @ChildrenOf
    public List<EmailMessage> getEmailMessages( EmailFolder folder ) {
        log.info( "getEmailMessages(): " + folder.getName() );
        return messages;
    }

}
