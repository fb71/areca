/* 
 * polymap.org
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

import java.util.logging.Logger;

import areca.ui.UIComponent.Consumer;
import areca.ui.teavm.MainHandler;

/**
 * 
 * @author falko
 */
public class App {

    private static final Logger LOG = Logger.getLogger( App.class.getSimpleName() );

    private static final App    INSTANCE = new App();
    
    public static App instance() {
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
        
    }
    
}
