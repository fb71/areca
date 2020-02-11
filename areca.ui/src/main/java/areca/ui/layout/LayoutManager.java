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
package areca.ui.layout;

import java.util.logging.Logger;

import areca.ui.UIComposite;

/**
 *
 * @author falko
 */
public abstract class LayoutManager {

    private static final Logger LOG = Logger.getLogger( LayoutManager.class.getSimpleName() );


    public abstract void layout( UIComposite composite );

}