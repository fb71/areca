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
package areca.rt.server.client;

import java.util.Objects;
import java.util.concurrent.Callable;

import org.teavm.jso.browser.Window;

import areca.common.Platform;
import areca.common.ProgressMonitor;
import areca.common.Promise;
import areca.common.base.Consumer.RConsumer;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Level;
import areca.common.log.LogFactory.Log;
import areca.rt.server.client.JSClient2ServerMessage.JSClickEvent;
import areca.rt.teavm.TeaApp;
import areca.ui.Size;
import areca.ui.component2.UIComponent;

/**
 * Ultra light client based on TeaVM runtime.
 *
 * @author Falko Bräutigam
 */
public class ClientApp
        extends TeaApp {

    private static final Log LOG = LogFactory.getLog( ClientApp.class );

    public static boolean           debug;

    /**
     *
     */
    public static void main( String[] args ) throws Exception {
        debug = Window.current().getLocation().getSearch().contains( "debug" );
        LOG.info( "Debug: %s", debug );
        LogFactory.DEFAULT_LEVEL = debug ? Level.INFO : Level.WARN;
        LogFactory.setClassLevel( ClientApp.class, Level.INFO );

        Promise.setDefaultErrorHandler( defaultErrorHandler() );
        try {
            // UI
            new ClientApp().createUI( rootWindow -> {
                var conn = new Connection( rootWindow );

                conn.enqueueClickEvent( JSResizeEvent.create( rootWindow, rootWindow.size.get() ) );
                rootWindow.size.onChange( (newSize, oldSize) -> {
                    if (!Objects.equals( newSize, oldSize )) {
                        LOG.info( "RESIZE: %s (%s)", newSize, oldSize );
                        conn.enqueueClickEvent( JSResizeEvent.create( rootWindow, newSize ) );
                    }
                });

                //SimpleBrowserHistoryStrategy.start( Pageflow.current() );

                conn.start();
            });
        }
        catch (Throwable e) {
            LOG.info( "Exception: %s -->", e );
            Throwable rootCause = Platform.rootCause( e );
            LOG.info( "Root cause: %s : %s", rootCause, rootCause.getMessage() );
            throw (Exception)rootCause;
        }
    }


    public static abstract class JSResizeEvent
            extends JSClickEvent {

        public static JSResizeEvent create( UIComponent window, Size newSize ) {
            var result = JSClickEvent.create();
            result.setComponentId( window.id() );
            result.setEventType( "resize" );
            result.setContent( String.format( "%s:%s", newSize.width(), newSize.height() ) );
            return result.cast();
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
            throw rootCauseForTeaVM( e );
        }
    }

    /**
     *
     */
    public static RConsumer<Throwable> defaultErrorHandler() {
        return (Throwable e) -> {
            if (e instanceof ProgressMonitor.CancelledException || e instanceof Promise.CancelledException) {
                LOG.info( "Operation cancelled." );
            }
            else if (debug) {
                throw rootCauseForTeaVM( e );
            }
            else {
                //Pageflow.current().open( new GeneralErrorPage( e ), null );
            }
        };
    }

    /**
     * get a meaningfull stracktrace in TeaVM
     */
    private static RuntimeException rootCauseForTeaVM( Throwable e ) {
        LOG.warn( "Exception: " + e );
        Throwable rootCause = Platform.rootCause( e );
        LOG.warn( "Root cause: " + rootCause, rootCause );
        if (e instanceof RuntimeException) {
            return (RuntimeException)rootCause;
        }
        else {
            return (RuntimeException)rootCause; // XXX
        }
    }

}
