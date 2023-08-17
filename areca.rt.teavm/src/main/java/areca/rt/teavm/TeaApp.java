/*
 * Copyright (C) 2023, the @authors. All rights reserved.
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
package areca.rt.teavm;

import org.teavm.jso.browser.Window;

import areca.common.base.Consumer;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.App;
import areca.ui.Size;
import areca.ui.component2.UIComposite;

/**
 *
 * @author Falko Bräutigam
 */
public class TeaApp
        extends App {

    private static final Log LOG = LogFactory.getLog( TeaApp.class );

    @Override
    public <E extends Exception> UIComposite createUI( Consumer<UIComposite,E> initializer ) throws E {
        return super.createUI( rootWindow -> {
            // XXX set the size of the root composite
            rootWindow.size.defaultsTo( () -> {
                var body = Window.current().getDocument().getBody();
                var size = Size.of( body.getClientWidth(), body.getClientHeight() );
                LOG.debug( "BODY: " + size );
                return size;
            });
            initializer.accept( rootWindow );
        });
    }

}
