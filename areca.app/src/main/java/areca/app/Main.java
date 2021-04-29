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
import areca.rt.teavm.html.TeaHtmlFactory;
import areca.ui.App;
import areca.ui.component.VisualClickFeedback;
import areca.ui.html.HtmlElement;
import areca.ui.pageflow.Pageflow;

/**
 *
 * @author Falko Bräutigam
 */
public class Main {

    private static final Log LOG = LogFactory.getLog( Main.class );


    public static void main( String[] args ) throws Exception {
//        TestsMain.main( args );
//        log.info( "done." );
//        return;

        try {
            HtmlElement.factory = new TeaHtmlFactory();
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
            VisualClickFeedback.start();
            Pageflow.start( rootWindow ).open( new StartPage(), null );


//            // PanGesture
//            var startPos = new MutableObject<Position>();
//            new PanGesture( rootWindow ).onEvent( ev -> {
//                LOG.info( "%s", ev.delta.get() );
//                var top = rootWindow.components.sequence().last().get();
//                switch (ev.status.get()) {
//                    case START: {
//                        startPos.setValue( top.position.get() );
//                        top.cssClasses.add( "Paned" );
//                        break;
//                    }
//                    case MOVE: {
//                        top.bordered.set( true );
//                        top.position.set( Position.of(
//                                startPos.getValue().x(),
//                                startPos.getValue().y() + ev.delta.get().y() ) );
//                        float opacity = (200f - ev.delta.get().y()) / 200f;
//                        top.htmlElm.styles.set( "opacity", opacity );
//                        break;
//                    }
//                    case END: {
//                        top.position.set( startPos.getValue() );
//                        top.htmlElm.styles.remove( "opacity" );
//                        top.cssClasses.remove( "Paned" );
//                        Platform.instance().schedule( 1000, () -> top.bordered.set( false ) );
//                    }
//                }
//            });
        });
    }
}
