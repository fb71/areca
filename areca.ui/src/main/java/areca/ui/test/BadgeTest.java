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
import areca.ui.Size;
import areca.ui.component2.Badge;
import areca.ui.component2.Button;
import areca.ui.component2.Events.EventType;
import areca.ui.component2.UIComposite;
import areca.ui.layout.RowConstraints;
import areca.ui.layout.RowLayout;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class BadgeTest {

    private static final Log LOG = LogFactory.getLog( BadgeTest.class );

    public static final ClassInfo<BadgeTest> info = BadgeTestClassInfo.instance();


    @Test( "Badge" )
    public void createRowLayout( UIComposite parent ) {
        var layout = new RowLayout() {{margins.set( Size.of( 15, 15 ) ); spacing.set( 5 );}};
        parent.layout.set( layout );

        parent.add( new Button() {{
            label.set( "Count" );
            layoutConstraints.set( new RowConstraints() {{width.set( 80 );}} );
            var badge = new Badge().content.set( "5" );
            addDecorator( badge );
            events.on( EventType.SELECT, ev -> {
                badge.content.set( "60" );
            });
        }});

        parent.add( new Button() {{
            label.set( "Appear" );
            layoutConstraints.set( new RowConstraints() {{width.set( 80 );}} );
            var badge = new Badge();
            addDecorator( badge );
            events.on( EventType.SELECT, ev -> {
                badge.content.set( "7" );
            });
        }});

        parent.add( new Button() {{
            label.set( "Disappear" );
            layoutConstraints.set( new RowConstraints() {{width.set( 90 );}} );
            var badge = new Badge().content.set( "5" );
            addDecorator( badge );
            events.on( EventType.SELECT, ev -> {
                badge.content.set( null );
                //badge.dispose();
            });
        }});

        parent.layout();
    }

}
