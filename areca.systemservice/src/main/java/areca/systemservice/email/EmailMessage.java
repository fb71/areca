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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 */
public class EmailMessage {

    private static final Log log = LogFactory.getLog( EmailMessage.class );

    private String name;

    public byte[] content = "Message!".getBytes();


    public EmailMessage( String name ) {
        this.name = name;
    }

    public String getName() {
        log.info( "getName(): " + name );
        return name;
    }

//    @ContentType
//    public String getContentType() {
//        log.info( "getContentType(): " + name );
//        return "text/plain";
//    }

//    @ContentLength
//    public int getContentLength() throws UnsupportedEncodingException {
//        return getContent().length;
//    }

//    @Get
//    public byte[] getContent( Object source, Response response ) {
//        log.info( "getContent(): source=" + source );
//        return "Message!".getBytes();
//    }
}