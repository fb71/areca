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
package areca.app.ui;

import static areca.ui.Orientation.VERTICAL;

import java.io.PrintWriter;
import java.io.StringWriter;

import areca.common.Platform.HttpServerException;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Color;
import areca.ui.Size;
import areca.ui.component2.Button;
import areca.ui.component2.Events.EventType;
import areca.ui.component2.Text;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.layout.RowConstraints;
import areca.ui.layout.RowLayout;
import areca.ui.pageflow.Page;
import areca.ui.pageflow.PageContainer;

/**
 *
 * @author Falko Bräutigam
 */
public class GeneralErrorPage
        extends Page {

    private static final Log LOG = LogFactory.getLog( GeneralErrorPage.class );

    protected Throwable     error;

    protected String        description;

    private PageContainer   ui;


    public GeneralErrorPage( Throwable error ) {
        this.error = error;
    }

    public GeneralErrorPage( Throwable error, String description ) {
        this.error = error;
        this.description = description;
    }


    @Override
    protected UIComponent doInit( UIComposite parent ) {
        ui = new PageContainer( this, parent );
        ui.title.set( "Failed" );
        ui.body.layout.set( new RowLayout()
                .orientation.set( VERTICAL ).fillWidth.set( true ).fillHeight.set( true )
                .spacing.set( 15 ).margins.set( Size.of( 10, 10 ) ) );

        if (description != null) {
            ui.body.add( new Text() {{
                layoutConstraints.set( new RowConstraints().height.set( 30 ) );
                content.set( description );
                bgColor.set( Color.rgb( 80, 80, 80 ) );
            }});
        }

        ui.body.add( new Text() {{
            format.set( Format.HTML );
            if (error instanceof HttpServerException) {
                content.set( String.format( "%s<br><br>%s", error.getMessage(), ((HttpServerException)error).responseBody ) );
            }
            else {
                var stack = new StringWriter( 4096 );
                error.printStackTrace( new PrintWriter( stack ) );
                LOG.info( "Stack size: %d", stack.toString().length() );
                //error.printStackTrace();

                content.set( String.format( "%s<br><pre style=\"white-space:pre-wrap\">%s\n\n%s</pre>", error.getMessage(), error, stack.toString() ) );
            }
        }});

        ui.body.add( new Button() {{
            layoutConstraints.set( new RowConstraints().height.set( 50 ) );
            label.set( "DISMISS" );
            events.on( EventType.SELECT, ev -> {
                pageSite.closePage();
            });
        }});
        return ui;
    }

}
