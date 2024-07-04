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
package areca.ui.viewer;

import areca.common.Assert;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.DatePicker;
import areca.ui.component2.Events.EventType;
import areca.ui.component2.UIComponent;
import areca.ui.viewer.model.Model;
import areca.ui.viewer.transform.Date2StringTransform;

/**
 *
 * @see Date2StringTransform
 * @author Falko Bräutigam
 */
public class DatePickerViewer
        extends Viewer<Model<String>> {

    private static final Log LOG = LogFactory.getLog( DatePickerViewer.class );

    protected DatePicker   datePicker;

    @Override
    public UIComponent create() {
        Assert.isNull( datePicker );
        datePicker = new DatePicker() {{
            events.on( EventType.TEXT, ev -> {
                LOG.info( "%s", value.get() );
                fireEvent( value.get(), null );
            });
            if (configurator != null) {
                configurator.accept( this );
            }
        }};
        model.subscribe( ev -> load() ).unsubscribeIf( () -> datePicker.isDisposed() );
        return datePicker;
    }

    @Override
    protected boolean isDisposed() {
        return Assert.notNull( datePicker, "No field has been created yet for this viewer." ).isDisposed();
    }

    @Override
    public String store() {
        var value = datePicker.value.opt().orNull();
        model.set( value );
        return value;
    }

    @Override
    public String load() {
        var value = model.get();
        datePicker.value.set( value );
        return value;
    }

}
