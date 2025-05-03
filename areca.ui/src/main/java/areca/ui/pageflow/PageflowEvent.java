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
import areca.ui.component2.UIComposite;
import areca.ui.pageflow.Page.PageSite;

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

    public Object       clientPage;

    public Page         page;

    public UIComposite  pageUI;

    public PageSite     pageSite;

    public EventType    type;

    public PageflowEvent( Pageflow source, EventType type ) {
        this( source, type, null, null, null, null );
    }

    public PageflowEvent( Pageflow source, EventType type, Page page, Object clientPage, UIComposite pageUI, PageSite pageSite ) {
        super( source );
        this.type = type;
        this.clientPage = clientPage;
        this.page = page;
        this.pageUI = pageUI;
        this.pageSite = pageSite;
    }

    @Override
    public Pageflow getSource() {
        return (Pageflow)super.getSource();
    }

    public Opt<Object> clientPage() {
        return Opt.of( clientPage );
    }
}
