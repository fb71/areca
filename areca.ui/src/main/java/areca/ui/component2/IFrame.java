/*
 * Copyright (C) 2024, the @authors. All rights reserved.
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

import java.util.EventObject;

import areca.ui.component2.Property.ReadWrite;

/**
 *
 * @author falko
 */
public class IFrame
        extends UIComponent {

    public ReadWrite<IFrame,String> src = Property.rw( this, "src" );

    /**
     * Use {@link #reload()} instaed of this property.
     */
    public ReadWrite<IFrame,Integer> reloadCount = Property.rw( this, "reloadCount", 0 );


    /**
     * Forces the iframe to reload its content.
     */
    public void reload() {
        reloadCount.set( reloadCount.get() + 1 );
    }


    /**
     * @implNote XXX better integration
     */
    public static class IFrameMsgEvent
            extends EventObject {

        public String msg;

        public IFrameMsgEvent( String msg ) {
            super( msg );
            this.msg = msg;
        }
    }

}
