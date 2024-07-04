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

import areca.ui.component2.Property.ReadWrite;

/**
 *
 * @see Date2StringTransform
 * @author Falko Bräutigam
 */
public class DatePicker
        extends UIComponent {

    public enum DateTime {
        DATE, TIME, DATETIME
    }

    /** */
    public ReadWrite<DatePicker,String> value = Property.rw( this, "value" );

    public ReadWrite<DatePicker,DateTime> dateTime = Property.rw( this, "dateTime", DateTime.DATETIME );

}
