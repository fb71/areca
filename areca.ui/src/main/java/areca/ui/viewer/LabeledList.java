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

import java.util.logging.Logger;

import areca.ui.component2.ListBase;
import areca.ui.component2.Property;
import areca.ui.component2.Property.ReadWrite;
import areca.ui.component2.Text;
import areca.ui.component2.UIComposite;
import areca.ui.layout.FillLayout;

/**
 * Standard list with text lines, an icon and status/action icons.
 *
 * @author Falko Bräutigam
 */
public class LabeledList<T>
        extends ListBase<T> {

    private static final Logger LOG = Logger.getLogger( LabeledList.class.getName() );

    public ReadWrite<LabeledList<T>,Labeler<T,String>> firstLineLabeler = Property.rw( this, "firstLineLabeler" );


    @Override
    protected void initRow( T data, UIComposite composite ) {
        composite.add( new Text(), text -> {
            composite.layout.set( new FillLayout() );
            text.content.set( firstLineLabeler.value().label( data ) );
        });
    }


    @Override
    protected void updateRow( T data, UIComposite composite ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

}
