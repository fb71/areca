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

import static areca.common.log.LogFactory.Level.INFO;

import areca.app.ui.StartPage;
import areca.common.Platform;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.rt.teavm.TeaPlatform;
import areca.rt.teavm.ui.UIComponentRenderer;
import areca.ui.App;
import areca.ui.component2.VisualActionFeedback;
import areca.ui.pageflow.Pageflow;
import areca.ui.test.GalleryMain;

/**
 *
 * @author Falko Bräutigam
 */
public class Main {

    private static final Log LOG = LogFactory.getLog( Main.class );

    public static void initLog() {
        LogFactory.DEFAULT_LEVEL = INFO;
        // LogFactory.setPackageLevel( areca.ui.component2.UIComponent.class, DEBUG );

        // LogFactory.setClassLevel( IDBUnitOfWork.class, DEBUG );
        // LogFactory.setClassLevel( UnitOfWorkImpl.class, DEBUG );
        // LogFactory.setClassLevel( areca.app.service.imap.ImapFolderSynchronizer.class, DEBUG );
        // LogFactory.setClassLevel( org.polymap.model2.test2.SimpleModelTest.class, DEBUG );
        // LogFactory.setClassLevel( org.polymap.model2.test2.AssociationsModelTest.class, DEBUG );
        // LogFactory.setClassLevel( areca.common.Promise.class, DEBUG );
    }


    public static void main( String[] args ) throws Exception {
        initLog();
        Platform.impl = new TeaPlatform();

//        TestsMain.main( args );
//        LOG.info( "done." );
//        return;

        try {
            UIComponentRenderer.start();

//            ModelRepo.init();
//            createApp();

            GalleryMain.createApp();
        }
        catch (Throwable e) {
            //System.out.println( "Exception: " + e + " --> " );
            Throwable rootCause = Platform.rootCause( e );
            System.out.println( "Root cause: " + rootCause );
            throw (Exception)rootCause;
        }
    }


    protected static void createApp() {
        App.instance().createUI( rootWindow -> {
            VisualActionFeedback.start();
            Pageflow.start( rootWindow ).open( new StartPage(), null, null );
        });
    }
}
