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

    private UIComposite     rootWindow;

    private UIComposite     header;

    private UIComposite     toolbar;

    private UIComposite     pages;


    public AppWindow( UIComposite composite ) {
        rootWindow = composite;
        rootWindow.layout.set( new AppWindowLayout() );

        header = rootWindow.add( new UIComposite() );

        toolbar = rootWindow.add( new UIComposite() );
        toolbar.layout.set( new FillLayout() );
        toolbar.add( new Button(), proto -> {
            proto.label.set( "toolbar" );
            proto.subscribe( ev -> proto.bordered.set( !proto.bordered.get() ) );
        });
        toolbar.bordered.set( true );

        pages = rootWindow.add( new UIComposite() );
        pages.layout.set( new PageStackLayout() );

        rootWindow.layout();
    }


    /**
     *
     */
    class AppWindowLayout
            extends LayoutManager {

        @Override
        public void layout( UIComposite composite ) {
            var size = rootWindow.size.get();

            header.position.set( Position.of( 0, 0 ) );
            header.size.set( Size.of( size.width(), 50 ) );

            toolbar.position.set( Position.of( 0, 50 ) );
            toolbar.size.set( Size.of( size.width(), 50 ) );

            pages.position.set( Position.of( 0, 100 ) );
            pages.size.set( Size.of( size.width(), size.height() - 100 ) );
        }

    }

}
