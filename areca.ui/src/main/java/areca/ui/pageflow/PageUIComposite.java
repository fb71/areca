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

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Position;
import areca.ui.Size;
import areca.ui.component.Button;
import areca.ui.component.UIComposite;
import areca.ui.layout.FillLayout;
import areca.ui.layout.LayoutManager;

/**
 * Provides the common user interface elements of a {@link Page}.
 *
 * @author Falko Bräutigam
 */
public class PageUIComposite
        extends UIComposite {

    static final Log log = LogFactory.getLog( PageUIComposite.class );

    public UIComposite      header;

    private UIComposite     toolbar;

    public UIComposite      body;


//    @Override
//    protected HtmlNode init( UIComposite parent ) {
//        super.init( parent );
//    }


    public PageUIComposite( UIComposite parent ) {
        parent.components.add( this );
        cssClasses.add( "PageUIComposite" );
        layout.set( new PageUILayout() );

        header = components.add( new UIComposite(), h -> {
            h.layout.set( new FillLayout() );
        });

        toolbar = components.add( new UIComposite(), tb -> {
            tb.layout.set( new FillLayout() );
            tb.components.add( new Button(), btn -> {
                btn.label.set( "Toolbar" );
                btn.bordered.set( false );
                btn.events.onSelection( ev -> btn.bordered.set( !btn.bordered.get() ) );
            });
            tb.bordered.set( true );
        });

        body = components.add( new UIComposite() );
    }


    /**
     *
     */
    class PageUILayout
            extends LayoutManager {

        public static final int HEADER_HEIGHT = 30;
        public static final int TOOLBAR_HEIGHT = 45;

        @Override
        public void layout( UIComposite composite ) {
            @SuppressWarnings("hiding")
            var size = PageUIComposite.this.clientSize.get();

            header.position.set( Position.of( 0, 0 ) );
            header.size.set( Size.of( size.width(), HEADER_HEIGHT ) );

            toolbar.position.set( Position.of( 0, HEADER_HEIGHT ) );
            toolbar.size.set( Size.of( size.width(), TOOLBAR_HEIGHT ) );

            var bodyHeight = HEADER_HEIGHT + TOOLBAR_HEIGHT;
            body.position.set( Position.of( 0,bodyHeight ) );
            body.size.set( Size.of( size.width(), size.height() - bodyHeight ) );
        }

    }

}
