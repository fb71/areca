/*
 * Copyright (C) 2020, the @authors. All rights reserved.
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
import areca.ui.component2.ColorPicker;
import areca.ui.component2.Events.EventType;
import areca.ui.component2.UIComponent;
import areca.ui.viewer.model.Model;

/**
 *
 * @author Falko Bräutigam
 */
//@RuntimeInfo
public class ColorPickerViewer
        extends Viewer<Model<String>> {

    private static final Log LOG = LogFactory.getLog( ColorPickerViewer.class );

    //public static final ClassInfo<ColorPickerViewer> info = ColorPickerViewerClassInfo.instance();

    protected ColorPicker   colorPicker;


    @Override
    public UIComponent create() {
        Assert.isNull( colorPicker );
        colorPicker = new ColorPicker() {{
            events.on( EventType.TEXT, ev -> {
                LOG.info( "%s", value.get() );
                fireEvent( value.get(), null );
            });
            if (configurator != null) {
                configurator.accept( this );
            }
        }};
        model.subscribe( ev -> load() ).unsubscribeIf( () -> colorPicker.isDisposed() );
        return colorPicker;
    }

    @Override
    protected boolean isDisposed() {
        return Assert.notNull( colorPicker, "No field has been created yet for this viewer." ).isDisposed();
    }

    @Override
    public String store() {
        var value = colorPicker.value.opt().orNull();
        model.set( value );
        return value;
    }

    @Override
    public String load() {
        var value = model.get();
        colorPicker.value.set( value );
        return value;
    }

}
