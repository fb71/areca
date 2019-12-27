/* 
 * polymap.org
 * Copyright (C) 2019, the @authors. All rights reserved.
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
package areca.ui;

import java.util.logging.Logger;

/**
 * 
 * @author falko
 *
 */
public class Button
        extends UIComponent {

    private static final Logger LOG = Logger.getLogger( Button.class.getSimpleName() );

    @SuppressWarnings("hiding")
    public static final Button  TYPE = new Button();

    public Property<String>     label = Property.create( this, "label" );
        
    
    public Button() {
    }

    
    public Button( String label ) {
        this.label.set( label );
    }

//    public <E extends Exception> Button props( Consumer<Button,E> task ) throws E {
//        return super.props( task );
//    }

}
