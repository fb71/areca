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

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import areca.common.event.EventListener;
import areca.common.event.EventManager;
import areca.ui.UIRenderEvent;

/**
 * 
 * @author falko
 */
public class MainHandler 
        implements EventListener<UIRenderEvent> {

    private static final Logger LOG = Logger.getLogger( MainHandler.class.getSimpleName() );

    private static final List<UIRenderer>   RENDERERS = Arrays.asList(
            new UICompositeRenderer(),
            new ButtonRenderer() );
    
    private static MainHandler              mainHandler;
    
    public static void start() {
        assert mainHandler == null;
        EventManager.instance().subscribe( mainHandler = new MainHandler() );
    }
    
    // instance *******************************************
    
    @Override
    public void handle( UIRenderEvent ev ) {
        LOG.info( "MainHandler: " + ev );
        for (UIRenderer renderer : RENDERERS) {
            renderer.handle( ev );
        }
    }

}
