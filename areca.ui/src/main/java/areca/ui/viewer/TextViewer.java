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

import java.util.EventObject;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component.Text;
import areca.ui.component.UIComponent;
import areca.ui.component.UIComposite;

/**
 *
 * @author Falko Bräutigam
 */
public class TextViewer
        extends Viewer<SingleValueAdapter<String>> {

    private static final Log log = LogFactory.getLog( TextViewer.class );

    @Override
    public UIComponent create( UIComposite container ) {
        return container.add( new Text(), t -> {
            t.text.set( model.getValue() );

            t.subscribe( (EventObject ev) -> {
                throw new RuntimeException( "Modifying text is not supported yet." );
                //model.setValue();
            });
        });
    }

}
