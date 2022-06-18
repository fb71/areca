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
package areca.ui.component2;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.Property.ReadWrite;

/**
 * A Badge must be created when the badged component is <b>created</b>. Set the
 * {@link #content} to null to make the badge invisible.
 *
 * @author Falko Bräutigam
 */
public class Badge
        extends UIComponentDecorator {

    private static final Log LOG = LogFactory.getLog( Badge.class );

    /**
     * The text to be shown by the badge. Null makes the badge to disappear.
     */
    public ReadWrite<Badge,String> content = Property.rw( this, "content" );


    public Badge( UIComponent decorated ) {
        super( decorated );
    }


    @Override
    public void dispose() {
        content.set( null );
        super.dispose();
    }

}
