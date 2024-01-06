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
package areca.rt.teavm;

import java.util.EventObject;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLBodyElement;
import org.teavm.jso.dom.html.HTMLElement;

import areca.common.Platform;
import areca.common.Session;
import areca.common.SessionScoper;
import areca.common.base.Consumer;
import areca.common.event.EventCollector;
import areca.common.event.EventManager;
import areca.common.event.IdleAsyncEventManager;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.rt.teavm.ui.UIComponentRenderer;
import areca.ui.App;
import areca.ui.Size;
import areca.ui.component2.EventHandlers;
import areca.ui.component2.UIComposite;
import areca.ui.component2.UIEventManager;

/**
 *
 * @author Falko Bräutigam
 */
public class TeaApp
        extends App {

    private static final Log LOG = LogFactory.getLog( TeaApp.class );

    public TeaApp() {
        LOG.info( "Setting default event managers..." );
        Platform.impl = new TeaPlatform();
        SessionScoper.setInstance( new SessionScoper.JvmSessionScoper() );
        Session.registerFactory( EventManager.class, () -> new IdleAsyncEventManager() );
        Session.registerFactory( UIEventManager.class, () -> new UIEventManager() );
        Session.registerFactory( EventHandlers.class, () -> new EventHandlers() );
        UIComponentRenderer.start();
    }

    /**
     * Automatically sets proper size of the rootWindow and handles update events.
     */
    @Override
    public <E extends Exception> UIComposite createUI( Consumer<UIComposite,E> initializer ) throws E {
        return super.createUI( rootWindow -> {
            // XXX set the size of the root composite
            HTMLBodyElement body = Window.current().getDocument().getBody();
            rootWindow.size.defaultsTo( () -> {
                return elementSize( body );
            });

            var throttle = new EventCollector<>( 750 );
            Window.current().addEventListener( "resize", ev -> {
                throttle.collect( new EventObject( body ), __ -> {
                    rootWindow.size.set( elementSize( body ) );
                });
            });

            rootWindow.size.onChange( (newSize,__) -> rootWindow.layout() );
            initializer.accept( rootWindow );
        });
    }

    protected Size elementSize( HTMLElement elm ) {
        var result = Size.of( elm.getClientWidth(), elm.getClientHeight() );
        LOG.info( "BODY: %s", result );
        return result;
    }

}
