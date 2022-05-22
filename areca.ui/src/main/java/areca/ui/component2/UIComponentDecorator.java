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

/**
 *
 * @author Falko Bräutigam
 */
public class UIComponentDecorator {

    private static final Log LOG = LogFactory.getLog( UIComponentDecorator.class );

    private UIComponent         decorated;


    public UIComponentDecorator( UIComponent decorated ) {
        this.decorated = decorated;
        this.decorated.attachDecorator( this );
    }


    public void dispose() {
        //decorated.dettachDecorator( );
    }

}
