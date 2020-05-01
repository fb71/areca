/* 
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

import java.util.EventObject;

/**
 * 
 * @author falko
 */
public class UIRenderEvent
        extends EventObject {

    public UIRenderEvent( Object source ) {
        super( source );
    }

    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + getSource() + "]";
    }


    /**
     * 
     */
    public static class ComponentCreatedEvent
        extends UIRenderEvent {

        public ComponentCreatedEvent( UIComponent source ) {
            super( source );
        }

        @Override
        public UIComponent getSource() {
            return (UIComponent)super.getSource();
        }
    }

    /**
     * 
     */
    public static class ComponentDestroyedEvent
        extends UIRenderEvent {

        public ComponentDestroyedEvent( UIComponent source ) {
            super( source );
        }
        
        @Override
        public UIComponent getSource() {
            return (UIComponent)super.getSource();
        }
    }

}
