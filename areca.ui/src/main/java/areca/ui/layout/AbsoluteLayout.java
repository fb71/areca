/*
 * Copyright (C) 2024, the @authors. All rights reserved.
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

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.UIComponent.CssStyle;
import areca.ui.component2.UIComposite;

/**
 *
 * @author Falko Bräutigam
 */
public abstract class AbsoluteLayout
        extends LayoutManager {

    private static final Log LOG = LogFactory.getLog( AbsoluteLayout.class );

    public static final CssStyle ABSOLUTE = CssStyle.of( "position", "absolute" );

    @Override
    public void layout( UIComposite composite ) {
        // XXX re-enable when first CSS LayoutManager is there
        // XXX also check UIComponentRenderer#hideWithoutPositionOrSize
//        for (var c : composite.components.value()) {
//            if (!c.styles.get().contains( ABSOLUTE )) {
//                c.styles.add( ABSOLUTE );
//            }
//        }
    }

}
