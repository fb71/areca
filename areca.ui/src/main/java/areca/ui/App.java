/*
 * Copyright (C) 2019, the @authors. All rights reserved.
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
package areca.ui;

import areca.common.Assert;
import areca.common.base.Consumer;
import areca.ui.component.UIComposite;
import areca.ui.html.HtmlNode;

/**
 *
 * @author falko
 */
public class App {

    private static App      instance;

    public static App instance() {
        return instance != null ? instance : (instance = new App());
    }

    // instance *******************************************

    private RootWindow           rootWindow;


    public <E extends Exception> UIComposite createUI( Consumer<UIComposite,E> initializer ) throws E {
        // MainRenderEventHandler.start();

        Assert.isNull( rootWindow );
        rootWindow = new RootWindow();
        rootWindow.init( null );

        initializer.accept( rootWindow );
        return rootWindow;
    }


    /**
     *
     */
    protected class RootWindow
            extends UIComposite {

        @Override
        public HtmlNode init( UIComposite newParent ) {
            return super.init( newParent );
        }
    }

}
