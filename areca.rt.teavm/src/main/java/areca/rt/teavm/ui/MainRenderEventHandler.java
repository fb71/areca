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

import areca.common.Assert;
import areca.common.event.EventListener;
import areca.common.event.EventManager;
import areca.ui.component.UIRenderEvent;

/**
 *
 * @author falko
 */
public class MainRenderEventHandler
        implements EventListener<UIRenderEvent> {

    private static final Logger LOG = Logger.getLogger( MainRenderEventHandler.class.getSimpleName() );

    private static final List<UIRenderer>   RENDERERS = Arrays.asList(
            new UICompositeRenderer(),
            new ButtonRenderer(),
            new TextRenderer() );


    private static MainRenderEventHandler              instance;


    public static void start() {
        Assert.isNull( instance );
        instance = new MainRenderEventHandler();
        EventManager.instance().subscribe( instance ).performIf( ev -> ev instanceof UIRenderEvent );
    }


    // instance *******************************************

    @Override
    public void handle( UIRenderEvent ev ) {
        LOG.info( "RENDER: " + ev );
        if (ev != null) {  // XXX remove if event handler param type check is in place
            for (UIRenderer renderer : RENDERERS) {
                renderer.handle( ev );
            }
        }
    }

}
