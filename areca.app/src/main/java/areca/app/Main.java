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
package areca.app;

import areca.common.Platform;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.rt.teavm.TeaPlatform;
import areca.rt.teavm.html.TeaHtmlImplFactory;
import areca.ui.App;
import areca.ui.component.Button;
import areca.ui.component.SelectionEvent;
import areca.ui.component.Text;
import areca.ui.html.HtmlElement;
import areca.ui.layout.GridLayout;
import areca.ui.pageflow.AppWindow;
import areca.ui.pageflow.PageStackLayout;

/**
 *
 * @author Falko Bräutigam
 */
public class Main {

    private static final Log log = LogFactory.getLog( Main.class );


    public static void main( String[] args ) throws Exception {
//        TestsMain.main( args );
//        log.info( "done." );
//        return;

        try {
            HtmlElement.factory = new TeaHtmlImplFactory();
            Platform.instance = new TeaPlatform();

            createApp();
            //UITestsMain.createGridLayoutApp();
        }
        catch (Throwable e) {
            //System.out.println( "Exception: " + e + " --> " );
            Throwable rootCause = e;
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }
            System.out.println( "Root cause: " + rootCause );
            throw (Exception)rootCause;
        }
    }


    protected static void createApp() {
        App.instance().createUI( rootWindow -> {
            rootWindow.layout.set( new PageStackLayout() );

            var appWindow = new AppWindow( rootWindow );

            appWindow.pages.layout.set( new GridLayout() {{spacing.set( 10 );}} );
            for (int i = 0; i < 20; i++) {
                var l = "" + i;
                appWindow.pages.components.add( new Button(), btn -> {
                    btn.label.set( l );
                    btn.htmlElm.styles.set( "border-radius", "20px" );
                    btn.onClick( (SelectionEvent ev) ->  {
                        var second = new AppWindow( rootWindow );
                        second.header.components.add( new Text(), t -> t.text.set( "Second") );
                        rootWindow.layout();
                    });
                });
            }
            rootWindow.layout();
        });
    }
}
