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


    // instance *******************************************

    private UIComposite         rootContainer;

    private Deque<Page>         pages = new ArrayDeque<>();


    protected Pageflow( UIComposite rootContainer ) {
        this.rootContainer = rootContainer;
        this.rootContainer.layout.set( new PageStackLayout() );
        new PageCloseGesture( rootContainer );
    }


    public void open( Page page, Page parent ) {
        Assert.isSame( parent, pages.peek() );
        pages.push( page );
        var pageContainer = page.init( rootContainer, new PageSite() {{
            // parent = Property.create( this, "parent", container );
        }});
        rootContainer.layout();
    }

}
