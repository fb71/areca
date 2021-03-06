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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.milton.http.fs.NullSecurityManager;
import io.milton.servlet.DefaultMiltonConfigurator;

/**
 *
 * @author Falko Bräutigam
 */
public class MiltonConfigurator
        extends DefaultMiltonConfigurator {

    private static final Log log = LogFactory.getLog( MiltonConfigurator.class );

    @Override
    protected void build() {
        log.debug( "build(): ..." );
        builder.setSecurityManager( new NullSecurityManager() );
        builder.setContextPath( "/areca.app-0.0.1-SNAPSHOT/webdav" );
        super.build();
    }

}
