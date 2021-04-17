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

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.rt.teavm.html.TeaHtmlImplFactory;
import areca.ui.App;
import areca.ui.html.HtmlElement;
import areca.ui.pageflow.AppWindow;

/**
 *
 * @author Falko Bräutigam
 */
public class Main {

    private static final Log log = LogFactory.getLog( Main.class );


    public static void main( String[] args ) throws Exception {
        // TestRunner
//        TestsMain.main( args );
//        log.info( "done." );
//        return;

        try {
            HtmlElement.factory = new TeaHtmlImplFactory();

            //createApp();
            UITestsMain.createGridLayoutApp();

//            var div = new HtmlElement( Type.DIV );
//            var btn = div.children.add( new HtmlButton() );
//            // btn.styles.set( "background-color", Color.WHITE );
//            btn.styles.set( "width", "50px" );
//            btn.styles.set( "height", "%spx", 50 );
//            btn.styles.set( "position", "absolute" );
//            btn.styles.set( "top", "%spx", 50 );
//            btn.styles.set( "left", "%spx", 50 );
//            log.info( "client= %s", btn.clientSize.get() );
//            log.info( "offset= %s", btn.offsetSize.get() );
//            log.info( "position= %s", btn.offsetPosition.get() );
//            var handle = btn.listeners.click( ev -> {
//                log.info( "position= %s", ev.clientPosition.get() );
//            });
//            btn.listeners.remove( handle );
//            btn.listeners.mouseMove( ev -> {
//                btn.styles.set( "left", "%spx", ev.clientPosition.get().x() );
//            });
//
//           // btn.children.append( new TextNode() );
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
            var appWindow = new AppWindow( rootWindow );

//            appWindow.layout.set( new GridLayout() {{spacing.set( 10 );}} );
//
//            for (int i = 0; i < 40; i++) {
//                var l = "" + i;
//                appWindow.add( new Button(), btn -> {
//                    btn.label.set( l );
//                    btn.subscribe( (SelectionEvent ev) ->  {
//                        appWindow.layout.set( (appWindow.layout.get() instanceof FillLayout)
//                                ? new GridLayout() : new FillLayout() );
//                        appWindow.layout();
//                    });
//                });
//            }

        });
    }
}
