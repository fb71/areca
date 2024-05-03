/*
 * Copyright (C) 2019-2022, the @authors. All rights reserved.
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

import areca.ui.component2.Events.EventType;
import areca.ui.component2.Property.ReadWrite;
import areca.ui.component2.Property.ReadWrites;

/**
 * Single (dropdown) or multiple (list) select field.
 * <p>
 * Issues {@link EventType#TEXT} events when {@link #value} changes.
 *
 * @author falko
 */
public class Select
        extends UIComponent {

    public ReadWrite<Select,Boolean> multiple = Property.rw( this, "multiple" );

    /**
     *
     */
    public ReadWrites<Select,String> options = Property.rws( this, "options" );

    /**
     * Issues {@link EventType#TEXT} when changed by the user.
     */
    public ReadWrite<Select,String> value = Property.rw( this, "value" );

    {
        bordered.set( true );
    }
}
