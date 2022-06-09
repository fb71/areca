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

import static areca.ui.Orientation.VERTICAL;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.ui.Size;
import areca.ui.component2.Button;
import areca.ui.component2.Events.EventType;
import areca.ui.component2.TextField;
import areca.ui.component2.UIComposite;
import areca.ui.layout.RowLayout;

@RuntimeInfo
public class TextInputTest {

    private static final Log LOG = LogFactory.getLog( TextInputTest.class );

    public static final ClassInfo<TextInputTest> info = TextInputTestClassInfo.instance();


    @Test( "TextField" )
    public void createRowLayout( UIComposite parent ) {
        parent.layout.set( new RowLayout()
                .orientation.set( VERTICAL ).margins.set( Size.of( 25, 15 ) ).spacing.set( 5 ).fillWidth.set( true ) );

        parent.add( new UIComposite() {{
            var textField = add( new TextField() {{
                content.set( "SELECT/Click to clear" );
                events.on( EventType.SELECT, ev -> {
                    content.set( "" );
                });
            }});
            add( new Button() {{
                events.on( EventType.SELECT, ev -> {
                    textField.content.set( "" );
                });
            }});
        }});
        parent.layout();
    }

}
