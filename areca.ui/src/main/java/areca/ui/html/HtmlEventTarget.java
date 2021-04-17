/*
 * Copyright (C) 2021, the @authors. All rights reserved.
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
package areca.ui.html;

import areca.common.base.Consumer;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Position;
import areca.ui.Property.ReadOnly;

/**
 *
 * @author Falko Bräutigam
 */
public abstract class HtmlEventTarget
        extends HtmlNode {

    private static final Log log = LogFactory.getLog( HtmlEventTarget.class );

    public HtmlEventListeners       listeners;


    /**
     *
     */
    public static abstract class HtmlEventListeners {

        public abstract ListenerHandle click( Consumer<HtmlMouseEvent,RuntimeException> handler );

        public abstract ListenerHandle mouseMove( Consumer<HtmlMouseEvent,RuntimeException> handler );

        public abstract void remove( ListenerHandle handle );

    }

    /**
     *
     */
    public static class ListenerHandle {

    }

    /**
     *
     */
    public static class HtmlEvent {

    }

    /**
     *
     */
    public static class HtmlMouseEvent
            extends HtmlEvent {

        public ReadOnly<Position>   clientPosition;
    }
}
