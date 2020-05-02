/*
 * Copyright (C) 2020, the @authors. All rights reserved.
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
package areca.app.ui;

import java.util.logging.Logger;

import areca.app.model.Anchor;
import areca.ui.context.ModelContext;
import areca.ui.context.UserInteractionContext;
import areca.ui.layout.FillLayout;
import areca.ui.viewer.ListModelAdapter;

/**
 *
 * @author Falko Bräutigam
 */
public class AnchorsMainContext
        extends UserInteractionContext {

    private static final Logger LOG = Logger.getLogger( AnchorsMainContext.class.getName() );

    protected FillLayout                    lm = new FillLayout();

    @ModelContext(type = Anchor.class)
    protected ListModelAdapter<Anchor>      anchors;


    @Override
    protected void init() {
        subscribeUIEvent( ev -> LOG.info( "Event: " + ev ) )
                .performIf( ev -> isSource( ev, anchors ) );

    }

}
