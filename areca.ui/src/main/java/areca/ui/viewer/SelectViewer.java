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

import java.util.List;

import areca.common.Assert;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.Events.EventType;
import areca.ui.component2.Select;
import areca.ui.component2.UIComponent;
import areca.ui.viewer.model.Model;

/**
 *
 * @author Falko Bräutigam
 */
public class SelectViewer
        extends Viewer<Model<String>> {

    private static final Log LOG = LogFactory.getLog( SelectViewer.class );

    protected Select select;

    protected List<String> options;


    public SelectViewer( List<String> options ) {
        this.options = options;
    }

    @Override
    public UIComponent create() {
        Assert.isNull( select );
        select = new Select() {{
            options.set( SelectViewer.this.options );
            events.on( EventType.TEXT, ev -> {
                fireEvent( value.get(), null );
            });
            if (configurator != null) {
                configurator.accept( this );
            }
        }};
        model.subscribe( ev -> load() ).unsubscribeIf( () -> select.isDisposed() );
        return select;
    }

    @Override
    protected boolean isDisposed() {
        return Assert.notNull( select, "No field has been created yet for this viewer." ).isDisposed();
    }

    @Override
    public String store() {
        var value = select.value.opt().orNull();
        model.set( value );
        return value;
    }

    @Override
    public String load() {
        var value = model.get();
        select.value.set( value );
        return value;
    }

}
