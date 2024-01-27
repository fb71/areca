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
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.ui.component2.Events.EventType;
import areca.ui.component2.TextField;
import areca.ui.component2.UIComponent;
import areca.ui.viewer.model.Model;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class TextFieldViewer
        extends Viewer<Model<String>> {

    private static final Log LOG = LogFactory.getLog( TextFieldViewer.class );

    public static final ClassInfo<TextFieldViewer> info = TextFieldViewerClassInfo.instance();

    protected TextField     textField;

    @Override
    public UIComponent create() {
        Assert.isNull( textField );
        textField = new TextField() {{
            events.on( EventType.TEXT, ev -> {
                LOG.info( "TextFieldViewer: %s", content.get() );
                fireEvent( content.get(), null );
            });
        }};
        model.subscribe( ev -> load() ).unsubscribeIf( () -> textField.isDisposed() );
        return textField;
    }

    @Override
    protected boolean isDisposed() {
        return Assert.notNull( textField, "No field has been created yet for this viewer." ).isDisposed();
    }

    @Override
    public void store() {
        model.set( textField.content.opt().orNull() );
    }

    @Override
    public void load() {
        textField.content.set( model.get() );
    }

}
