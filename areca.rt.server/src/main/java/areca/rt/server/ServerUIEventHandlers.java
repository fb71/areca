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

import areca.common.Session;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.EventHandlers;
import areca.ui.component2.Property.PropertyChangedEvent;
import areca.ui.component2.UIEventManager;

/**
 *
 * @author Falko Bräutigam
 */
public class ServerUIEventHandlers
        extends EventHandlers {

    private static final Log LOG = LogFactory.getLog( ServerUIEventHandlers.class );

    @Override
    @SuppressWarnings("unchecked")
    public void fireEvent( PropertyChangedEvent<Object> ev ) {
        // -> ServerUIEventManager -> UIEventCollector
        Session.instanceOf( UIEventManager.class ).publish( ev );

        //Assert.isEqual( 0, handlers.length );
        for (var handler : handlers) {
            try {
                handler.accept( ev );
            }
            catch (Exception e) {
                throw (RuntimeException)e;
            }
        }
    }

}
