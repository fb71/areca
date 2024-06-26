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
package areca.ui;

import areca.common.base.Consumer;
import areca.ui.component2.Button;
import areca.ui.component2.Events.UIEvent;
import areca.ui.component2.Property;
import areca.ui.component2.Property.ReadWrite;

/**
 *
 * @author Falko Bräutigam
 */
public class Action {

    public ReadWrite<?,String>      label = Property.rw( this, "label" );

    /** Often displayed as the tooltip of a button. */
    public ReadWrite<?,String>      description = Property.rw( this, "description" );

    /** A ligature or numeric character reference of a Material Icon. */
    public ReadWrite<?,String>      icon = Property.rw( this, "icon" );

    /** */
    public ReadWrite<?,Boolean>     enabled = Property.rw( this, "icon", true );

    /** */
    public ReadWrite<?,Button.Type> type = Property.rw( this, "icon", Button.Type.ACTION );

    public ReadWrite<?,Consumer<UIEvent,?>> handler = Property.rw( this, "handler" );

    /** Left to right ascending */
    public ReadWrite<?,Integer>     order = Property.rw( this, "order" );

}
