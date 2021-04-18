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
 *
 * @author Falko Bräutigam
 */
public class AppWindow {

    private static final Log log = LogFactory.getLog( AppWindow.class );

    private UIComposite     container;

    public UIComposite      header;

    private UIComposite     toolbar;

    public UIComposite      pages;


    public AppWindow( UIComposite parent ) {
        container = parent.components.add( new UIComposite() );
        container.cssClasses.add( "AppWindow" );
        container.layout.set( new AppWindowLayout() );

        header = container.components.add( new UIComposite(), h -> {
            h.layout.set( new FillLayout() );
        });

        toolbar = container.components.add( new UIComposite(), tb -> {
            tb.layout.set( new FillLayout() );
            tb.components.add( new Button(), btn -> {
                btn.label.set( "toolbar" );
                btn.bordered.set( false );
                btn.onClick( ev -> btn.bordered.set( !btn.bordered.get() ) );
            });
            tb.bordered.set( true );
        });

        pages = container.components.add( new UIComposite() );
        // pages.layout.set( new PageStackLayout() );

        //container.layout();
    }

    /**
     *
     */
    class AppWindowLayout
            extends LayoutManager {

        public static final int HEADER_HEIGHT = 30;
        public static final int TOOLBAR_HEIGHT = 40;

        @Override
        public void layout( UIComposite composite ) {
            var size = container.clientSize.get();

            header.position.set( Position.of( 0, 0 ) );
            header.size.set( Size.of( size.width(), HEADER_HEIGHT ) );

            toolbar.position.set( Position.of( 0, HEADER_HEIGHT ) );
            toolbar.size.set( Size.of( size.width(), TOOLBAR_HEIGHT ) );

            var bodyHeight = HEADER_HEIGHT + TOOLBAR_HEIGHT;
            pages.position.set( Position.of( 0,bodyHeight ) );
            pages.size.set( Size.of( size.width(), size.height() - bodyHeight ) );
        }

    }

}
