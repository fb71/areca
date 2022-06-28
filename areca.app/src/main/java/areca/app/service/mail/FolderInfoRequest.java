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

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public class FolderInfoRequest
        extends MailRequest<FolderInfoRequest.FolderInfoResponse> {

    private static final Log LOG = LogFactory.getLog( FolderInfoRequest.class );

    public static final String  FILE_NAME = "folder.info";

    public FolderInfoRequest( RequestParams params, String folderName ) {
        super( params );
        setPath( folderName, FILE_NAME );
    }


    /**
     *
     */
    public static interface FolderInfoResponse
            extends MailRequest.Response, JSObject {

        @JSProperty( "exists" )
        public boolean exists();

        @JSProperty( "unread" )
        public int unread();

        @JSProperty( "count" )
        public int count();
    }
}
