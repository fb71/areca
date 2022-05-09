/*
 * Copyright (C) 2019-2022, the @authors. All rights reserved.
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
import areca.ui.component2.UIComposite;

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

        initializer.accept( rootWindow );
        return rootWindow;
    }

    public RootWindow rootWindow() {
        return Assert.notNull( rootWindow );
    }

    /**
     *
     */
    public class RootWindow
            extends UIComposite {
    }

}
