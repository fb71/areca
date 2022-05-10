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

import org.teavm.jso.browser.Window;

import areca.app.model.ModelRepo;
import areca.app.ui.StartPage;
import areca.common.Platform;
import areca.common.base.Consumer;
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

        String hash = Window.current().getLocation().getHash();
        LOG.info( "URL hash: " + hash );

        // TestsMain
        if (hash.equals( "#tests" )) {
            TestsMain.main( args );
            LOG.info( "done." );
        }
        // Gallery
        else if (hash.equals( "#gallery" )) {
            catchAll( __ -> {
                UIComponentRenderer.start();
                GalleryMain.createApp();
            } );
        }
        // no #hash
        else if (!hash.isBlank()) {
            throw new RuntimeException( "Unknown hash: " + hash );
        }
        // app
        else {
            catchAll( __ -> {
                ModelRepo.init();
                UIComponentRenderer.start();
                App.instance().createUI( rootWindow -> {
                    VisualActionFeedback.start();
                    Pageflow.start( rootWindow ).open( new StartPage(), null, null );
                });
            });
        }
    }


    protected static void catchAll( Consumer<Void,?> code ) throws Exception {
        try {
            code.accept( null );
        }
        catch (Throwable e) {
            LOG.debug( "Exception: %s -->", e );
            Throwable rootCause = Platform.rootCause( e );
            LOG.debug( "Root cause: %s : %s", rootCause, rootCause.getMessage() );
            throw (Exception)rootCause;
        }
    }

}
