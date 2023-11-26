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

import java.util.EventObject;

import areca.common.Promise;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.UIEventManager;

/**
 *
 * @author Falko Bräutigam
 */
public class ServerUIEventManager
        extends UIEventManager {

    private static final Log LOG = LogFactory.getLog( ServerUIEventManager.class );

    @Override
    public void publish( EventObject ev ) {
        for (var handler : handlers) {
            handler.perform( ev );
       }
    }

    @Override
    public Promise<Void> publish2( EventObject ev ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

}
