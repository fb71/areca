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

import org.teavm.jso.browser.Window;

import org.polymap.model2.engine.UnitOfWorkImpl;
import org.polymap.model2.store.tidbstore.IDBUnitOfWork;

import areca.common.Platform;
import areca.common.base.Consumer;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Level;
import areca.common.log.LogFactory.Log;
import areca.common.testrunner.AsyncAwareTestRunner;
import areca.common.testrunner.LogDecorator;
import areca.rt.teavm.TeaPlatform;
import areca.rt.teavm.testapp.HtmlTestRunnerDecorator;
import areca.rt.teavm.ui.UIComponentRenderer;
import areca.ui.Size;
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
        // LogFactory.setClassLevel( org.polymap.model2.test2.SimpleModelTest.class, DEBUG );
        // LogFactory.setClassLevel( org.polymap.model2.test2.AssociationsModelTest.class, DEBUG );
        // LogFactory.setClassLevel( areca.common.Promise.class, DEBUG );
    }


    @SuppressWarnings("unchecked")
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
                var doc = Window.current().getDocument();
                var appSize = Size.of( doc.getBody().getClientWidth(), doc.getBody().getClientHeight() );
                LOG.info( "BODY: " + appSize );
                GalleryMain.createApp( appSize );
            });
        }
        // #imap
        else if (hash.equals( "#imap" )) {
            catchAll( __ -> {
                LogFactory.setClassLevel( areca.app.service.imap.ImapFolderSynchronizer.class, DEBUG );
                LogFactory.setClassLevel( areca.app.service.carddav.CarddavSynchronizer.class, DEBUG );
                new AsyncAwareTestRunner()
                        //.addTests( areca.app.service.imap.ImapTest.info )
                        .addTests( areca.app.service.carddav.CardDavTest.info )
                        .addDecorators( HtmlTestRunnerDecorator.info, LogDecorator.info )
                        .run();
            });
        }
        // no #hash
        else if (!hash.isBlank()) {
            throw new RuntimeException( "Unknown hash:  " + hash );
        }
        // app
        else {
            catchAll( __ -> {
                LogFactory.setClassLevel( IDBUnitOfWork.class, Level.DEBUG );
                LogFactory.setClassLevel( UnitOfWorkImpl.class, Level.DEBUG );

                ArecaApp.instance().createUI();
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
