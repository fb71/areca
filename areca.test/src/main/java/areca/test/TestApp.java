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
package areca.test;

import org.teavm.jso.browser.Window;

import areca.common.Platform;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Level;
import areca.common.log.LogFactory.Log;
import areca.rt.server.client.ClientApp;
import areca.rt.teavm.TeaApp;
import areca.rt.teavm.ui.basic.BasicComponentRenderer;
import areca.ui.component2.Text;

/**
 *
 * @author Falko Bräutigam
 */
public class TestApp
        extends TeaApp {

    private static final Log LOG = LogFactory.getLog( TestApp.class );

    private static boolean debug;

    protected TestApp() {
        BasicComponentRenderer.start();
    }

    /**
     *
     */
    public static void main( String[] args ) throws Exception {
        debug = true; //Window.current().getLocation().getSearch().contains( "debug" );
        LOG.info( "DEBUG: %s", debug );
        LogFactory.DEFAULT_LEVEL = debug ? Level.INFO : Level.WARN;
        LogFactory.setClassLevel( ClientApp.class, Level.INFO );

       // Promise.setDefaultErrorHandler( defaultErrorHandler() );
        try {
            // Tests
            var path = Window.current().getLocation().getPathName();
            LOG.info( "URL hash: %s", path );
            var hash = Window.current().getLocation().getHash();
            LOG.info( "URL hash: %s", hash );

            if (hash.equals( "#tests" ) || hash.equals( "#test" )) {
                UnitTestsPage.doRunTests();
                return;
            }
            // UI
            new TestApp().createUI( rootWindow -> {

                rootWindow.add( new Text() {{
                    content.set( "TestApp!" );
                }});

//                var conn = new Connection( rootWindow );
//
//                conn.enqueueClickEvent( JSResizeEvent.create( rootWindow, rootWindow.size.get() ) );
//                rootWindow.size.onChange( (newSize, oldSize) -> {
//                    if (!Objects.equals( newSize, oldSize )) {
//                        LOG.info( "RESIZE: %s (%s)", newSize, oldSize );
//                        conn.enqueueClickEvent( JSResizeEvent.create( rootWindow, newSize ) );
//                    }
//                });
//
//                //SimpleBrowserHistoryStrategy.start( Pageflow.current() );
//
//                conn.start();
            });
        }
        catch (Throwable e) {
            LOG.info( "Exception: %s -->", e );
            Throwable rootCause = Platform.rootCause( e );
            LOG.info( "Root cause: %s : %s", rootCause, rootCause.getMessage() );
            throw (Exception)rootCause;
        }
    }

}
