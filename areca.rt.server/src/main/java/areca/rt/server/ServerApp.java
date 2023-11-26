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
import areca.ui.App;
import areca.ui.Size;
import areca.ui.component2.EventHandlers;
import areca.ui.component2.UIComposite;
import areca.ui.component2.UIEventManager;

/**
 * An Areca app running on the server. :)
 *
 * @author Falko Bräutigam
 */
public abstract class ServerApp
        extends App {

    private static final Log LOG = LogFactory.getLog( ServerApp.class );

    public static void init() throws Exception {
        LOG.info( "Setting default event managers..." );
        Session.registerFactory( EventManager.class, () -> new SameStackEventManager() ); // XXX
        Session.registerFactory( UIEventManager.class, () -> new ServerUIEventManager() );
        Session.registerFactory( EventHandlers.class, () -> new ServerUIEventHandlers() );
        Platform.impl = new ServerPlatform();
    }

    // instance *******************************************

    public abstract void createUI();


    /**
     * Automatically sets proper size of the rootWindow and handles update events.
     */
    @Override
    public <E extends Exception> UIComposite createUI( Consumer<UIComposite,E> initializer ) throws E {
        return super.createUI( rootWindow -> {
            // XXX set the size of the root composite
            rootWindow.size.defaultsTo( () -> {
                return Size.of( 450, 500 );
            });

//            var throttle = new EventCollector<>( 750 );
//            Window.current().addEventListener( "resize", ev -> {
//                throttle.collect( new EventObject( body ), __ -> {
//                    rootWindow.size.set( elementSize( body ) );
//                });
//            });

            rootWindow.size.onChange( (newSize,__) -> rootWindow.layout() );
            initializer.accept( rootWindow );
        });
    }

}
