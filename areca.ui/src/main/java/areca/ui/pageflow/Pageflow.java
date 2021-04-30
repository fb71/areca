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
package areca.ui.pageflow;

import java.util.ArrayDeque;
import java.util.Deque;
import areca.common.Assert;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component.UIComponent;
import areca.ui.component.UIComposite;
import areca.ui.pageflow.Page.PageSite;

/**
 *
 * @author Falko Bräutigam
 */
public class Pageflow {

    private static final Log log = LogFactory.getLog( Pageflow.class );

    private static Pageflow     instance;

    public static Pageflow start( UIComposite rootContainer ) {
        Assert.isNull( instance );
        return instance = new Pageflow( rootContainer );
    }

    public static Pageflow current() {
        return Assert.notNull( instance, "Pageflow not start()ed yet." );
    }

    private class PageData {
        Page        page;
        PageSite    site;
        UIComponent container;
    }

    // instance *******************************************

    private UIComposite         rootContainer;

    private Deque<PageData>     pages = new ArrayDeque<>();


    protected Pageflow( UIComposite rootContainer ) {
        this.rootContainer = rootContainer;
        this.rootContainer.layout.set( new PageStackLayout() );
        new PageCloseGesture( rootContainer );
    }


    public void open( Page _page, Page parent ) {
        Assert.that( pages.isEmpty() || parent == pages.peek().page );
        var _pageSite = new PageSite() {{
        }};
        var _pageContainer = _page.init( rootContainer, _pageSite );
        pages.push( new PageData() {{page = _page; site = _pageSite; container = _pageContainer;}} );
        rootContainer.layout();
    }


    public void close( Page page ) {
        Assert.isSame( page, pages.peek().page );
        var pageData = pages.pop();
        pageData.page.dispose();
        if (!pageData.container.isDisposed()) {
            pageData.container.dispose();
        }
        rootContainer.layout();
    }
}
