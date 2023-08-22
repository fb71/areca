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
package areca.ui.pageflow;

import java.util.EventObject;

import areca.common.base.Opt;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public class PageflowEvent
        extends EventObject {

    private static final Log LOG = LogFactory.getLog( PageflowEvent.class );

    public enum EventType {
       INITIALIZING, INITIALIZED, PAGE_OPENING, PAGE_OPENED, PAGE_CLOSING, PAGE_CLOSED
    }

    public Opt<Object>      page;

    public EventType        type;

    public PageflowEvent( Pageflow source, Object page, EventType type ) {
        super( source );
        this.page = Opt.of( page );
        this.type = type;
    }

    @Override
    public Pageflow getSource() {
        return (Pageflow)super.getSource();
    }

}
