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
package areca.rt.teavm.ui.mdb;

import areca.common.event.EventHandler;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.ui.component2.Button;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComponentEvent;
import areca.ui.component2.UIComponentEvent.ComponentConstructedEvent;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class MDBTheme {

    private static final Log LOG = LogFactory.getLog( MDBTheme.class );

    public static final ClassInfo<MDBTheme> TYPE = MDBThemeClassInfo.instance();

    /**
     *
     */
    public static void _start() {
        MDBTheme theme = new MDBTheme();
        UIComponentEvent.manager().subscribe( theme )
                .performIf( ev -> ev instanceof ComponentConstructedEvent );
    }


    @EventHandler( ComponentConstructedEvent.class )
    public void elementCreaded( ComponentConstructedEvent ev ) {
        if (ev.getSource() instanceof UIComponent) {
            var c = (UIComponent)ev.getSource();
            LOG.debug( "%s -> ...", c.getClass().getSimpleName() );
            //
            if (c instanceof Button) {
                c.cssClasses.setThemeClasses( "btn", "btn-primary" );
            }
        }
    }

}
