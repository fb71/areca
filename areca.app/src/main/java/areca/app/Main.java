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

import areca.common.Platform;
import areca.common.base.Consumer;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Level;
import areca.common.log.LogFactory.Log;
import areca.common.test.SequenceOpTest;
import areca.common.testrunner.AsyncAwareTestRunner;
import areca.common.testrunner.LogDecorator;
import areca.common.testrunner.TestRunner;
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

    @SuppressWarnings("unchecked")
    public static void main( String[] args ) throws Exception {
        LogFactory.DEFAULT_LEVEL = INFO;
        Platform.impl = new TeaPlatform();

        String hash = Window.current().getLocation().getHash();
        LOG.info( "URL hash: " + hash );

        // TestsMain
        if (hash.equals( "#tests" )) {
            TestsMain.main( args );
            LOG.info( "done." );
        }
        // runtime
        else if (hash.equals( "#runtime" )) {
            catchAll( __ -> {
                new TestRunner()
                        //.addTests( areca.common.test.UIEventManagerTest.info )
                        //.addTests( areca.rt.teavm.test.TeavmRuntimeTest.info )
                        //.addTests( areca.common.test.RuntimeTest.info )
                        //.addTests( areca.common.test.AsyncTests.info )
                        .addTests( areca.common.test.SchedulerTest.info )
                        //.addTests( areca.common.test.IdleAsyncEventManagerTest.info )
                        //.addTests( areca.common.test.AsyncEventManagerTest.info )
                        .addDecorators( HtmlTestRunnerDecorator.info, LogDecorator.info )
                        .run();
            });
        }
        // Gallery
        else if (hash.equals( "#gallery" )) {
            catchAll( __ -> {
                //UIComponentEvent.manager;
                UIComponentRenderer.start();
                var doc = Window.current().getDocument();
                var appSize = Size.of( doc.getBody().getClientWidth(), doc.getBody().getClientHeight() );
                LOG.info( "BODY: " + appSize );
                GalleryMain.createApp( appSize );
            });
        }
        // #m2
        else if (hash.equals( "#m2" )) {
            catchAll( __ -> {
                new TestRunner()
                        //.addTests( org.polymap.model2.test2.SimpleModelTest.info )
                        .addTests( org.polymap.model2.test2.SimpleQueryTest.info )
                        .addTests( org.polymap.model2.test2.RuntimeTest.info )
                        .addTests( org.polymap.model2.test2.AssociationsTest.info )
                        .addTests( org.polymap.model2.test2.ComplexModelTest.info )
                        .addDecorators( HtmlTestRunnerDecorator.info, LogDecorator.info )
                        .run();
            });
        }
        // #mail
        else if (hash.equals( "#mail" )) {
            catchAll( __ -> {
                LogFactory.setPackageLevel( areca.app.service.mail.MailRequest.class, DEBUG );
                new AsyncAwareTestRunner()
                        .addTests( areca.app.service.mail.MailTest.info )
                        .addDecorators( HtmlTestRunnerDecorator.info, LogDecorator.info )
                        //.addTests( areca.app.service.carddav.CardDavTest.info )
                        .run();
            });
        }
        // #carddav
        else if (hash.equals( "#carddav" )) {
            catchAll( __ -> {
                LogFactory.setPackageLevel( areca.app.service.carddav.CarddavService.class, DEBUG );
                new AsyncAwareTestRunner()
                        .addTests( areca.app.service.carddav.CarddavTest.info )
                        .addDecorators( HtmlTestRunnerDecorator.info, LogDecorator.info )
                        .run();
            });
        }
        // #matrix
        else if (hash.equals( "#matrix" )) {
            catchAll( __ -> {
                LogFactory.setPackageLevel( areca.app.service.matrix.MatrixService.class, DEBUG );
                //LogFactory.setClassLevel( IDBUnitOfWork.class, Level.DEBUG );
                new AsyncAwareTestRunner()
                        .addTests( areca.app.service.matrix.MatrixTest.info )
                        .addDecorators( HtmlTestRunnerDecorator.info, LogDecorator.info )
                        .run();
            });
        }
        // #bench
        else if (hash.equals( "#bench" )) {
            catchAll( __ -> {
                LogFactory.setPackageLevel( areca.common.event.AsyncEventManager.class, DEBUG );
                new TestRunner()
                        .addTests( BenchTest.info, SequenceOpTest.info )
                        .addDecorators( HtmlTestRunnerDecorator.info, LogDecorator.info )
                        .run()
                        .run();
            });
        }
        // unknown #hash
        else if (!hash.isBlank()) {
            throw new RuntimeException( "Unknown hash:  " + hash );
        }
        // app
        else {
            catchAll( __ -> {
                LogFactory.DEFAULT_LEVEL = Level.WARN;
                var debug = Window.current().getLocation().getSearch().contains( "debug" );
                if (debug) {
                    LogFactory.DEFAULT_LEVEL = Level.INFO;
//                    LogFactory.setClassLevel( IDBUnitOfWork.class, Level.DEBUG );
//                    LogFactory.setClassLevel( UnitOfWorkImpl.class, Level.DEBUG );
//                    LogFactory.setClassLevel( areca.app.service.carddav.CarddavSynchronizer.class, DEBUG );
//                    LogFactory.setClassLevel( areca.app.service.imap.ImapFolderSynchronizer.class, DEBUG );
//                    LogFactory.setClassLevel( areca.app.service.Message2ContactAnchorSynchronizer.class, DEBUG );
//                    LogFactory.setClassLevel( areca.app.service.Message2PseudoContactAnchorSynchronizer.class, DEBUG );
                }
                ArecaApp.instance().createUI( debug );
            });
        }
    }


    protected static void catchAll( Consumer<Void,?> code ) throws Exception {
        try {
            code.accept( null );
        }
        catch (Throwable e) {
            LOG.info( "Exception: %s -->", e );
            Throwable rootCause = Platform.rootCause( e );
            LOG.info( "Root cause: %s : %s", rootCause, rootCause.getMessage() );
            throw (Exception)rootCause;
        }
    }

}
