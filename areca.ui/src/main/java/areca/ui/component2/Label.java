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
 * A label decoration.
 * <p/>
 * Does not work for {@link TextField}! (yet?)
 *
 * @author Falko Bräutigam
 */
public class Label
        extends UIComponentDecorator {

    private static final Log LOG = LogFactory.getLog( Label.class );

    /**
     * The text to be shown by the label. Null makes the label to disappear.
     */
    public ReadWrite<Label,String> content = new ReadWrite<>( this, "content" );


    public Label( UIComponent decorated ) {
        super( decorated );
        if (decorated instanceof TextField) {
            throw new IllegalArgumentException( "Unfortunatelly Label does not work directly with TextField :(" );
        }
    }


    @Override
    public void dispose() {
        content.set( null );
        super.dispose();
    }

}
