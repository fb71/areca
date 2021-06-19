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

import static areca.common.log.LogFactory.Level.DEBUG;
import static areca.common.log.LogFactory.Level.INFO;

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

    public static void initLog() {
        LogFactory.DEFAULT_LEVEL = INFO;
        // LogFactory.setClassLevel( IDBUnitOfWork.class, DEBUG );
        // LogFactory.setClassLevel( UnitOfWorkImpl.class, DEBUG );
        LogFactory.setClassLevel( areca.app.service.imap.ImapFolderSynchronizer.class, DEBUG );
        // LogFactory.setClassLevel( org.polymap.model2.test2.SimpleModelTest.class, DEBUG );
        LogFactory.setClassLevel( org.polymap.model2.test2.AssociationsModelTest.class, DEBUG );
        // LogFactory.setClassLevel( areca.common.Promise.class, DEBUG );
    }


    public static void main( String[] args ) throws Exception {
        initLog();
        HtmlElement.factory = new TeaHtmlFactory();
        Platform.impl = new TeaPlatform();

        TestsMain.main( args );
        // LOG.info( "done." );
        return;

//        try {
//            createApp();
//            //UITestsMain.createGridLayoutApp();
//        }
//        catch (Throwable e) {
//            //System.out.println( "Exception: " + e + " --> " );
//            Throwable rootCause = Platform.rootCause( e );
//            System.out.println( "Root cause: " + rootCause );
//            throw (Exception)rootCause;
//        }
    }


    protected static void createApp() {
        App.instance().createUI( rootWindow -> {
            VisualClickFeedback.start();
            Pageflow.start( rootWindow ).open( new StartPage(), null );
        });
    }
}
