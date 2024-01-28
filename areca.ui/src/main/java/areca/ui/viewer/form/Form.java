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

import java.util.ArrayList;
import java.util.List;

import areca.common.base.Sequence;
import areca.common.event.EventListener;
import areca.ui.component2.UIComponent;
import areca.ui.viewer.Viewer.ViewerInputChangeEvent;
import areca.ui.viewer.model.ModelBase;

/**
 *
 * @author Falko Bräutigam
 */
public class Form {

    protected List<FieldContext<?>> fields = new ArrayList<>();

    protected List<EventListener<ViewerInputChangeEvent>> listeners = new ArrayList<>();


    public FieldBuilder<ModelBase> newField() {
        return new FieldContext<>() {
            { fields.add( this ); }
            @Override
            public UIComponent create() {
                var result = super.create();
                listeners.forEach( l -> subscribe( l ) );
                return result;
            }
        };
    }


    public boolean isChanged() {
        return Sequence.of( fields ).anyMatches( f -> f.isChanged() );
    }


    public boolean isValid() {
        return Sequence.of( fields ).allMatch( f -> f.isValid() );
    }


    public void subscribe( EventListener<ViewerInputChangeEvent> l ) {
        fields.forEach( f -> f._viewer().subscribe( new EventListener<ViewerInputChangeEvent>() {
            @Override public void handle( ViewerInputChangeEvent ev ) {
                l.handle( ev );
            }
        }));
        listeners.add( l );
    }


    public void submit() {
        fields.forEach( f -> f._viewer().store() );
    }

    public void revert() {
        fields.forEach( f -> f._viewer().load() );
    }

    public void load() {
        fields.forEach( f -> f._viewer().load() );
    }

}
