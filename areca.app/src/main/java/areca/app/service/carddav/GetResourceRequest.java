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

import areca.common.Platform;
import areca.common.Platform.HttpResponse;
import areca.common.Platform.HttpServerException;
import areca.common.Promise;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public class GetResourceRequest {

    private static final Log LOG = LogFactory.getLog( GetResourceRequest.class );

    private DavResource res;


    public GetResourceRequest( DavResource res ) {
        this.res = res;
    }


    public Promise<HttpResponse> submit() {
        var xhr = Platform.xhr( "GET", "http?uri=" + res.url() );

        res.username.ifPresent( user ->
                xhr.authenticate( user, res.pwd.get() ) );

        return xhr.submit()
                .map( response -> {
                    if (response.status() > 299) {
                        throw new HttpServerException( response.status(), response.text() );
                    }
                    return response;
                });
    }
}
