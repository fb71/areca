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
import areca.ui.component2.Events.EventType;
import areca.ui.component2.TextField;
import areca.ui.component2.UIComponent;
import areca.ui.viewer.model.Model;

/**
 *
 * @author Falko Bräutigam
 */
public class TextFieldViewer
        extends Viewer<Model<String>> {

    private static final Log LOG = LogFactory.getLog( TextFieldViewer.class );

    protected TextField     textField;


    @Override
    public UIComponent create() {
        Assert.isNull( textField );
        textField = new TextField() {{
            events.on( EventType.TEXT, ev -> {
                //LOG.info( "%s", content.get() );
                fireEvent( content.get(), null );
            });
            if (configurator != null) {
                configurator.accept( this );
            }
        }};
        model.subscribe( ev -> load() ).unsubscribeIf( () -> textField.isDisposed() );
        return textField;
    }

    @Override
    protected boolean isDisposed() {
        return Assert.notNull( textField, "No field has been created yet for this viewer." ).isDisposed();
    }

    @Override
    public String store() {
        var value = textField.content.opt().orNull();
        model.set( value );
        return value;
    }

    @Override
    public String load() {
        var value = model.get();
        textField.content.set( value );
        return value;
    }

}
