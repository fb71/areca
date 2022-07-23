/*
 * Copyright (C) 2022, the @authors. All rights reserved.
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
package areca.app.service.mail;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.json.JSON;

import areca.app.service.mail.MessageSendRequest.Message;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public class MessageAppendRequest
        extends MailRequest<MessageAppendRequest.MessageAppendResponse> {

    private static final Log LOG = LogFactory.getLog( MessageAppendRequest.class );

    public static final String  FILE_NAME = "message.append";

    public MessageAppendRequest( RequestParams params, String folderName, Message msg ) {
        super( params );
        setPath( folderName, FILE_NAME );
        this.content = JSON.stringify( msg );
    }

    /**
     *
     */
    public interface MessageAppendResponse
            extends MailRequest.Response, JSObject {

        @JSProperty( "count" )
        public int count();
    }

}
