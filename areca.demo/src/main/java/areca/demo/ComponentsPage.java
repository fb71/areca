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

import static areca.ui.component2.Events.EventType.SELECT;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import areca.common.Platform;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.ui.Color;
import areca.ui.Size;
import areca.ui.component2.Button;
import areca.ui.component2.Label;
import areca.ui.component2.Text;
import areca.ui.component2.TextField;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.layout.FillLayout;
import areca.ui.layout.MaxWidthLayout;
import areca.ui.layout.RowConstraints;
import areca.ui.layout.RowLayout;
import areca.ui.pageflow.Page;
import areca.ui.pageflow.Page.PageSite;
import areca.ui.pageflow.PageContainer;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class ComponentsPage {

    private static final Log LOG = LogFactory.getLog( ComponentsPage.class );

    public static final ClassInfo<ComponentsPage> INFO = ComponentsPageClassInfo.instance();

    @Page.Context
    protected PageSite          pageSite;

    @Page.Part
    protected PageContainer     ui;

    protected List<TextField>   textFields = new ArrayList<>();

    protected Text              resultText;

    @Page.CreateUI
    protected UIComposite createUI( UIComposite parent ) {
        ui.init( parent ).title.set( "Components" );

        ui.body.layout.set( MaxWidthLayout.width( 350 ).fillHeight.set( true ) );
        ui.body.add( new UIComposite() {{
            layout.set( RowLayout.defaults().vertical().margins( Size.of( 0, 40) ).spacing( 30 ).fillWidth( true ) );

            add( decorate( "Firstname", new TextField() {{
                content.set( "Ellen ???" );
                textFields.add( this );
            }}));
            add( decorate( "Lastname", new TextField() {{
                content.set( "Ripley" );
                textFields.add( this );
            }}));

            add( resultText = new Text() {{
                layoutConstraints.set( RowConstraints.height( 35 ) );
                bordered.set( true );
                styles.add( CssStyle.of( "text-align", "center" ) );
                styles.add( CssStyle.of( "padding", "8px" ) );
            }});

            add( new Button() {{
                label.set( "CLOSE" );
                events.on( SELECT, ev -> pageSite.close() );
            }});

            Platform.schedule( 500, () -> checkResult() );
        }});
        return ui;
    }


    @SuppressWarnings("deprecation")
    protected void checkResult() {
        var result = Sequence.of( textFields ).map( field -> field.content.$() ).reduce( "", (r,n) -> r + " " + n );
        resultText.content.set( result );

        var distance = StringUtils.getJaroWinklerDistance( result, "Ellen Louise Ripley" );
        LOG.info( "distance: %s", distance );
        resultText.bgColor.set( Color.rgb( 40, (int)(40 + ((distance - 0.6) * 100)), 40 ) );

        if (!resultText.isDisposed()) {
            Platform.schedule( 500, () -> checkResult() );
        }
    }


    protected UIComponent decorate( String _label, TextField t ) {
        return new UIComposite() {{
            addDecorator( new Label().content.set( _label ) );
            layoutConstraints.set( RowConstraints.height( 35 ) );
            layout.set( FillLayout.defaults() );
            add( t );
        }};
    }
}
