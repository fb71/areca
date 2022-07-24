/*
 * Copyright (C) 2021, the @authors. All rights reserved.
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
package areca.app.service.carddav;

import java.util.Date;
import org.teavm.jso.dom.xml.Document;

import areca.common.Platform;
import areca.common.Platform.HttpServerException;
import areca.common.Promise;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public class PropfindRequest {

    private static final Log LOG = LogFactory.getLog( PropfindRequest.class );

    private DavResource res;

//    public final ReadOnly<PropfindRequest,String>     url;
//
//    public final ReadWrite<PropfindRequest,String>    username = Property.create( this, "username", (String)null );
//
//    public final ReadWrite<PropfindRequest,String>    pwd = Property.create( this, "pwd", (String)null );


    public PropfindRequest( DavResource res ) {
        this.res = res;
    }


//    @SuppressWarnings("hiding")
//    public PropfindRequest auth( String username, String pwd ) {
//        this.username.set( username );
//        this.pwd.set( pwd );
//        return this;
//    }



    @SuppressWarnings("deprecation")
    public Promise<DavResource[]> submit() {
        var xhr = Platform.xhr( "PROPFIND", "http?uri=" + res.url() )
                .addHeader( "Content-Type", "application/xml" ) // ; charset=utf-8
                .addHeader( "Depth", "1" );

        res.username.ifPresent( user ->
                xhr.authenticate( user, res.pwd.get() ) );

        return xhr.submit()
                .map( response -> {
                    LOG.debug( "Status: %s", response.status() );
                    if (response.status() > 299) {
                        throw new HttpServerException( response.status(), response.text() );
                    }

                    Document xml = (Document)response.xml();
                    var responses = xml.getElementsByTagNameNS( "DAV:", "response" );
                    LOG.debug( "responses: %s", responses.getLength() );

                    DavResource[] result = new DavResource[responses.getLength() - 1];
                    for (int i = 1; i < responses.getLength(); i++) {
                        var resp = responses.get( i );
                        result[i - 1] = new DavResource( res ) {{
                            href = resp.getElementsByTagNameNS( "DAV:", "href" ).item( 0 ).getTextContent();
                            lastModified = new Date( resp.getElementsByTagNameNS( "DAV:", "getlastmodified" ).item( 0 ).getTextContent() );
                        }};
                    }
                    return result;
                });
    }

}
