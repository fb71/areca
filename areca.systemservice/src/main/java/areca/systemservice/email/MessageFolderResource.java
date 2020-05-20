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

import java.util.Collections;

import javax.mail.Message;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.milton.resource.Resource;

/**
 *
 * @author Falko Bräutigam
 */
public class MessageFolderResource
        extends FolderResourceBase {

    private static final Log log = LogFactory.getLog( MessageFolderResource.class );

    private Message      message;

    public MessageFolderResource( Message message ) {
        this.message = message;
    }

    @Override
    public String getName() {
        return "message-" + message.getMessageNumber();
    }

    @Override
    protected Iterable<? extends Resource> createChildren() throws Exception {
        return Collections.emptyList();
    }

}
