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

import io.milton.annotations.Name;

/**
 *
 * @author Falko Bräutigam
 */
public class EmailFolder {

    private static final Log log = LogFactory.getLog( EmailFolder.class );

    private String name;

    public EmailFolder( String name ) {
        this.name = name;
    }

    @Name
    public String getName() {
        log.info( "getName(): " + name );
        return name;
    }

}
