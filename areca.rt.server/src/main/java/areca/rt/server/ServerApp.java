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
package areca.rt.server;

import areca.common.Platform;
import areca.common.Session;
import areca.common.base.Consumer;
import areca.common.event.EventManager;
import areca.common.event.SameStackEventManager;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.rt.server.servlet.ArecaUIServer;
import areca.ui.App;
import areca.ui.Size;
import areca.ui.component2.EventHandlers;
import areca.ui.component2.UIComposite;
import areca.ui.component2.UIEventManager;

/**
 * An Areca app running on the server. :)
 * <p/>
 * There is one instance per Session.
 *
 * @author Falko Bräutigam
 */
public abstract class ServerApp
        extends App {

    private static final Log LOG = LogFactory.getLog( ServerApp.class );

    /**
     * Static global initializer called by the {@link ArecaUIServer#init()}.
     * <p/>
     * Sub-classes must not call super method!
     */
    public static void init() throws Exception {
        Platform.impl = new ServerPlatform();
        LOG.info( "Platform: %s", Platform.impl.getClass().getSimpleName() );
        Session.registerFactory( EventManager.class, () -> new SameStackEventManager() ); // XXX
        Session.registerFactory( UIEventManager.class, () -> new ServerUIEventManager() );
        Session.registerFactory( EventHandlers.class, () -> new ServerUIEventHandlers() );
        LOG.info( "Default event manager: %s", SameStackEventManager.class.getSimpleName() );
    }

    /**
     * Static global destructor called by the {@link ArecaUIServer#destroy()}.
     * <p/>
     * Sub-classes must not call super method!
     */
    public static void dispose() throws Exception {
        if (Platform.impl != null) {
            Platform.impl.dispose();
            Platform.impl = null;
        }
    }

    // instance *******************************************

    public abstract void createUI();


    /**
     * Automatically sets proper size of the rootWindow and handles update events.
     */
    @Override
    public <E extends Exception> UIComposite createUI( Consumer<UIComposite,E> initializer ) throws E {
        return super.createUI( rootWindow -> {
            // XXX the very first request (containing the size of the rootWindow)
            // is handled *after* main() method run; so the rootWIndow needs a reasonable size
            // although it is re-sized right after
            rootWindow.size.defaultsTo( () -> {
                return Size.of( 500, 500 );
            });

            rootWindow.size.onChange( (newSize,__) -> rootWindow.layout() );
            initializer.accept( rootWindow );
        });
    }

}
