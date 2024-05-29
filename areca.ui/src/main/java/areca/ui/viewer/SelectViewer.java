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
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.Events.EventType;
import areca.ui.component2.Property;
import areca.ui.component2.Property.ReadWrite;
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

    /**
     * The option/label for null values.
     */
    public ReadWrite<SelectViewer,String> nullOption = Property.rw( this, "null" );

    protected Select select;

    protected List<String> options;


    public SelectViewer( Iterable<String> options ) {
        this.options = Sequence.of( options ).toList();
    }

    /**
     *
     * @param options
     * @param nullOption {@link #nullOption}
     */
    public SelectViewer( Iterable<String> options, String nullOption ) {
        this.options = Sequence.of( options ).toList();
        this.nullOption.set( nullOption );
    }


    @Override
    public UIComponent create() {
        Assert.isNull( select );
        select = new Select() {{
            var all = nullOption.opt().map( o -> Sequence.of( o ) ).orElse( Sequence.of() )
                    .concat( SelectViewer.this.options )
                    .toList();
            options.set( all );

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
        if (value.equals( nullOption.$() )) {
            model.set( null );
        }
        else {
            model.set( value );
        }
        return value;
    }


    @Override
    public String load() {
        var value = model.get();
        if (value == null && nullOption.opt().isPresent()) {
            select.value.set( nullOption.$() );
        }
        else {
            select.value.set( value );
        }
        return value;
    }

}
