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
import areca.rt.teavm.ui.TeaApp;
import areca.ui.pageflow.AppWindow;

/**
 *
 * @author Falko Bräutigam
 */
public class Main {

    private static final Log log = LogFactory.getLog( Main.class );


    public static void main( String[] args ) throws Exception {
        // TestRunner
        TestsRunnerMain.main( args );
        log.info( "done." );
//        return;

        try {
            //FirstGroovy.test();
            //createApp();
            ComponentTestsMain.createGridLayoutApp();
        }
        catch (Throwable e) {
            System.out.println( "Exception: " + e + " --> " );
            Throwable rootCause = e;
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }
            System.out.println( "Root cause: " + rootCause );
            throw (Exception)rootCause;
        }
    }


    protected static void createApp() {
        TeaApp.instance().createUI( rootWindow -> {
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
