/*
 * Copyright (C) 2022, the @authors. All rights reserved.
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
package areca.ui.test;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.ui.Color;
import areca.ui.Orientation;
import areca.ui.Size;
import areca.ui.component2.Button;
import areca.ui.component2.Events.EventType;
import areca.ui.component2.Text;
import areca.ui.component2.UIComposite;
import areca.ui.layout.RowLayout;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class RowLayoutTest {

    private static final Log LOG = LogFactory.getLog( RowLayoutTest.class );

    public static final ClassInfo<RowLayoutTest> info = RowLayoutTestClassInfo.instance();


    @Test( "Row" )
    public void createRowLayout( UIComposite parent ) {
        var layout = new RowLayout() {{margins.set( Size.of( 10, 10 ) ); spacing.set( 10 ); fillWidth.set( true );}};
        parent.layout.set( layout );

        parent.add( new Text() {{
            content.set( "UI2 :) -- " + bordered );
            bordered.set( true );
            events.on( EventType.SELECT, ev -> {
                LOG.warn( "CLICK: " + ev );
                var current = size.value();
                size.set( Size.of( current.width() - 20, current.height() - 20 ) );
                bgColor.set( Color.rgb( 0x30, 0x40, 0x50 ) );
                layout.orientation.set( Orientation.VERTICAL );
               // parent.layout.set( layout );
                parent.layout();
            });
        }});

        parent.add( new Button() {{
            LOG.warn( "CSS: " + cssClasses );
            label.set( "+++" );
            events.on( EventType.SELECT, ev -> {
                this.label.set( "Hola!" );
                // Platform.schedule( 1000, () -> this.dispose() );
                layout.orientation.set( Orientation.HORIZONTAL );
                parent.layout();
            });
        }});
        parent.layout();
    }

}
