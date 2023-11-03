/*
 * Copyright (C) 2023, the @authors. All rights reserved.
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
package areca.aws.logs;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Falko Bräutigam
 */
public class HttpRequestEvent {

    public static HttpRequestEvent prepare( HttpServletRequest req, HttpServletResponse resp ) {
        return new HttpRequestEvent( req, resp );
    }

    // instance *******************************************

    private HttpServletResponse resp;

    public HttpRequestEvent( HttpServletRequest req, HttpServletResponse resp ) {
        this.resp = resp;
        method = req.getMethod();
        url = String.format( "%s://%s:%s/%s", req.getScheme(), req.getServerName(), req.getServerPort(), req.getRequestURI() );
    }

    public HttpRequestEvent complete() {
        time = System.currentTimeMillis() - timestamp.getTime();
        status = resp.getStatus();
        return this;
    }

    public Date timestamp = new Date();

    public long time;

    public String vhost;

    public String url;

    public String method;

    public String forward;

    public int status;

    public String exception;

}
