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
package areca.rt.teavm.ui;

import java.util.logging.Logger;

import areca.ui.App;
import areca.ui.UIComponent.Consumer;
import areca.ui.UIComposite;

/**
 *
 * @author falko
 */
public class TeaApp
        extends App {

    private static final Logger LOG = Logger.getLogger( TeaApp.class.getSimpleName() );

    private static final TeaApp    INSTANCE = new TeaApp();

    public static TeaApp instance() {
        return INSTANCE;
    }

    // instance *******************************************

    private RootWindow           rootWindow;


    public <E extends Exception> UIComposite createUI( Consumer<UIComposite,E>... initializer ) throws E {
        MainHandler.start();

        assert rootWindow == null;
        rootWindow = new RootWindow();
        rootWindow.init( null );

        assert initializer.length < 2;
        if (initializer.length == 1) {
            initializer[0].perform( rootWindow );
        }
        return rootWindow;
    }


    /**
     *
     */
    protected class RootWindow
            extends UIComposite {

        @Override
        public void init( UIComposite newParent ) {
            super.init( newParent );
        }
    }

}
