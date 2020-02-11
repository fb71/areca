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

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLDocument;

import areca.common.event.EventListener;
import areca.ui.UIRenderEvent;

/**
 *
 * @author falko
 */
public abstract class UIRenderer
        implements EventListener<UIRenderEvent> {

    private static final Logger LOG = Logger.getLogger( UIRenderer.class.getSimpleName() );

    @Override
    public void handle( UIRenderEvent ev ) {
    }

    protected static HTMLDocument doc() {
        return Window.current().getDocument();
    }

}
