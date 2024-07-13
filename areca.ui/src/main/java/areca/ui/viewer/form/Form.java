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
package areca.ui.viewer.form;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import areca.common.Assert;
import areca.common.Platform;
import areca.common.base.Sequence;
import areca.common.event.EventListener;
import areca.common.event.EventManager;
import areca.ui.viewer.Viewer.ViewerInputChangeEvent;
import areca.ui.viewer.model.ModelBase;

/**
 *
 * @author Falko Bräutigam
 */
public class Form {

    protected Map<String, FieldContext<?>> fields = new HashMap<>();

    protected int fieldNameCount = 0;

    public FieldBuilder<ModelBase> newField() {
        return newField( String.valueOf( fieldNameCount++ ) );
    }

    public FieldBuilder<ModelBase> newField( String fieldName ) {
        var result = new FieldContext<>();
        if (fields.put( fieldName, result ) != null) {
            throw new IllegalArgumentException( "fieldName already exists: " + fieldName );
        }
        return result;
    }

    public FormField field( String fieldName ) {
        return Assert.notNull( fields.get( fieldName ), "No such fieldName: " + fieldName );
    }

    protected Sequence<FieldContext<?>,RuntimeException> fields() {
        return Sequence.of( fields.values() );
    }

    public boolean isChanged() {
        return fields().anyMatches( f -> f.isChanged() );
    }

    public boolean isValid() {
        return fields().allMatch( f -> f.isValid() );
    }

    public void subscribe( EventListener<ViewerInputChangeEvent> l ) {
        EventManager.instance().subscribe( ev ->
                // XXX Form.subscribe() might be called *before* the viewers are initialized
                // so we must wait Viewers have processed their event handlers, in order to
                // see correct results for isChanged() and isValid()
                Platform.async( () -> l.handle( (ViewerInputChangeEvent)ev ) ) )

                .performIf( ViewerInputChangeEvent.class, ev -> {
                    return fields().anyMatches( f -> f._viewer() == ev.getSource() );
                } );
    }


    public void submit() {
        fields().forEach( f -> f.store() );
    }

    public void revert() {
        fields().forEach( f -> f.load() );
    }

    public void load() {
        // allow fields to be created as a result of loading a (list) field
        var toBeProcessed = new HashSet<>( fields.values() );
        var processed = new HashSet<FieldContext<?>>();
        while (!toBeProcessed.isEmpty()) {
            for (var f : toBeProcessed) {
                f.load();
            }
            processed.addAll( toBeProcessed );
            toBeProcessed = new HashSet<>( fields.values() );
            toBeProcessed.removeAll( processed );
        }
    }

}
