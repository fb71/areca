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
package areca.demo;

import areca.common.Platform;
import areca.common.Promise;
import areca.common.Scheduler.Priority;
import areca.common.base.Opt;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * Provides access to "content" files on the server.
 *
 * @author Falko Bräutigam
 */
public class CMS {

    private static final Log LOG = LogFactory.getLog( CMS.class );

    public static final String URL_BASE = "cms/";
    public static final String SUFFIX_MD = ".md";

    public File file( String _name ) {
        return new File() {{name = _name; url = URL_BASE + _name + SUFFIX_MD;}};
    }

    /**
     *
     */
    public static class File {

        protected String    name;

        protected String    url;

        public String name() {
            return name;
        }

        public String url() {
            return url;
        }

        public Promise<Opt<String>> content() {
            LOG.debug( "Loading: %s ...", url );
            return Platform.xhr( "GET", url )
                    .submit()
                    .priority( Priority.BACKGROUND )
                    .map( response -> response.status() < 300 ? Opt.of( response.text() ) : Opt.absent() );
        }
    }

}
