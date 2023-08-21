/*
 * Copyright (C) 2023, the @authors. All rights reserved.
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
package areca.demo;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.ui.Orientation;
import areca.ui.Size;
import areca.ui.component2.Button;
import areca.ui.component2.Events.EventType;
import areca.ui.component2.UIComposite;
import areca.ui.layout.RowLayout;
import areca.ui.pageflow.Page;
import areca.ui.pageflow.Page.PageSite;
import areca.ui.pageflow.PageContainer;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class LayoutPage {

    private static final Log LOG = LogFactory.getLog( LayoutPage.class );

    public static final ClassInfo<LayoutPage> INFO = LayoutPageClassInfo.instance();

    @Page.Context
    protected PageSite          pageSite;

    @Page.Part
    protected PageContainer     ui;


    @Page.CreateUI
    protected UIComposite createUI( UIComposite parent ) {
        ui.init( parent ).title.set( "Components" );

        var layout = new RowLayout().margins( Size.of( 5, 5 ) ).spacing( 5 ).fillWidth( true );
        ui.body.layout.set( layout );

        ui.body.add( new Button() {{
            label.set( "Horizontal" );
            styles.add( CssStyle.of( "overflow", "hidden" ) );
            events.on( EventType.SELECT, ev -> {
                layout.orientation.set( Orientation.HORIZONTAL );
                parent().layout();
            });
        }});
        ui.body.add( new Button() {{
            label.set( "Vertical" );
            styles.add( CssStyle.of( "overflow", "hidden" ) );
            events.on( EventType.SELECT, ev -> {
                layout.orientation.set( Orientation.VERTICAL );
                parent().layout();
            });
        }});
        ui.body.add( new Button() {{
            label.set( "Fill Width (" + layout.fillWidth.value() + ")" );
            styles.add( CssStyle.of( "overflow", "hidden" ) );
            events.on( EventType.SELECT, ev -> {
                layout.fillWidth.set( !layout.fillWidth.value() );
                parent().layout();
            });
        }});
        ui.body.add( new Button() {{
            label.set( "Fill Height (" + layout.fillHeight.value() + ")" );
            styles.add( CssStyle.of( "overflow", "hidden" ) );
            events.on( EventType.SELECT, ev -> {
                layout.fillHeight.set( !layout.fillHeight.value() );
                label.set( "fill h (" + layout.fillHeight.value() + ") " );
                parent().layout();
            });
        }});
        return ui;
    }

}
