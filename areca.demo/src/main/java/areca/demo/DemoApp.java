/*
 * Copyright (C) 2023, the @authors. All rights reserved.
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
package areca.demo;

import java.util.concurrent.Callable;

import org.teavm.jso.browser.Window;

import areca.common.Platform;
import areca.common.ProgressMonitor;
import areca.common.Promise;
import areca.common.base.Consumer.RConsumer;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Level;
import areca.common.log.LogFactory.Log;
import areca.rt.teavm.SimpleBrowserHistoryStrategy;
import areca.rt.teavm.TeaApp;
import areca.rt.teavm.ui.basic.BasicComponentRenderer;
import areca.ui.component2.UIComposite;
import areca.ui.layout.MaxWidthLayout;
import areca.ui.pageflow.Page;
import areca.ui.pageflow.Pageflow;

/**
 * The main entry point of the application.
 *
 * @author Falko Bräutigam
 */
public class DemoApp
        extends TeaApp {

    private static final Log LOG = LogFactory.getLog( DemoApp.class );

    public static boolean   debug;

    protected DemoApp() {
        //MDBComponentRenderer.start();
        BasicComponentRenderer.start();
    }

    /**
     *
     */
    public static void main( String[] args ) throws Exception {
        debug = Window.current().getLocation().getSearch().contains( "debug" );
        LOG.info( "DEBUG: %s", debug );
        LogFactory.DEFAULT_LEVEL = debug ? Level.INFO : Level.WARN;
        Promise.setDefaultErrorHandler( defaultErrorHandler() );

        try {
            // DemoApp UI
            new DemoApp().createUI( rootWindow -> {
                rootWindow.layout.set( MaxWidthLayout.width( 680 ).fillHeight.set( true ) );

                var pageflowContainer = rootWindow.add( new UIComposite() {{cssClasses.add( "MaxWidth" );}} );
                rootWindow.layout();

                Pageflow.start( pageflowContainer )
                        .create( new StartPage() )
                        .putContext( new CMS(), Page.Context.DEFAULT_SCOPE )
                        .open();

                SimpleBrowserHistoryStrategy.start( Pageflow.current() );
            });
        }
        catch (Throwable e) {
            LOG.info( "Exception: %s -->", e );
            Throwable rootCause = Platform.rootCause( e );
            LOG.info( "Root cause: %s : %s", rootCause, rootCause.getMessage() );
            throw (Exception)rootCause;
        }
    }

    /**
     * Helps to handle exceptions in any code that is not handled
     * by default (UI clicks are handled by default for example).
     */
    public static <R> R catchAll( Callable<R> callable ) {
        try {
            return callable.call();
        }
        catch (Throwable e) {
            LOG.warn( "Exception: " + e );
            Throwable rootCause = Platform.rootCause( e );
            LOG.warn( "Root cause: " + rootCause, rootCause );
            if (e instanceof RuntimeException) {
                throw (RuntimeException)rootCause;
            }
            else {
                throw (RuntimeException)rootCause; // XXX
            }
        }
    }

    /**
     *
     */
    private static RConsumer<Throwable> defaultErrorHandler() {
        return (Throwable e) -> {
            if (e instanceof ProgressMonitor.CancelledException || e instanceof Promise.CancelledException) {
                LOG.info( "Operation cancelled." );
            }
            else if (debug) {
                // get a meaningfull stracktrace in TeaVM
                throw (RuntimeException)e;
            }
            else {
                //Pageflow.current().open( new GeneralErrorPage( e ), null );
            }
        };
    }

}
