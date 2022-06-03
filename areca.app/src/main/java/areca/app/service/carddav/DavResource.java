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

import areca.common.base.Opt;

/**
 *
 * @author Falko Bräutigam
 */
public class DavResource {

    public static DavResource create( String _baseUrl, String _href ) {
        return new DavResource() {{
            this.baseUrl = _baseUrl;
            this.href = _href;
        }};
    }

    // instance *******************************************

    protected Opt<String>   username = Opt.absent();

    protected Opt<String>   pwd = Opt.absent();

    public String           baseUrl;

    public String           href;

    public Date             lastModified;


    private DavResource() {};

    protected DavResource( DavResource other ) {
        this.baseUrl = other.baseUrl;
        this.username = other.username;
        this.pwd = other.pwd;

    }

    @SuppressWarnings("hiding")
    public DavResource auth( String username, String pwd ) {
        this.username = Opt.of( username );
        this.pwd = Opt.of( pwd );
        return this;
    }

    public String url() {
        return baseUrl + href;
    }

    @Override
    public String toString() {
        return "DavResource [href=" + href + ", lastModified=" + lastModified + "]";
    }

}
