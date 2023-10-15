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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.logging.Logger;

import areca.ui.component2.Property;
import areca.ui.component2.Property.ReadWrite;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;

/**
 *
 * @author falko
 */
public abstract class LayoutManager {

    private static final Logger LOG = Logger.getLogger( LayoutManager.class.getSimpleName() );

    public ReadWrite<?,Comparator<? extends UIComponent>> componentOrder = Property.rw( this, "componentOrder" );


    public abstract void layout( UIComposite composite );


    public int computeMinWidth( int height ) {
        throw new RuntimeException( "computeMinWidth() is not implemented for: " + getClass().getName() );
    }

    public int computeMinHeight( int width ) {
        throw new RuntimeException( "computeMinHeight() is not implemented for: " + getClass().getName() );
    }


    /**
     * The components of the given {@link UIComposite} ordered as specified via
     * {@link #componentOrder}.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected Collection<UIComponent> orderedComponents( UIComposite composite) {
        return componentOrder.opt()
                .ifPresentMap( order -> {
                    var result = new ArrayList<>( composite.components.value() );
                    // XXX this is bad; we trust the caller to have correct types as children and order
                    Comparator strippedOrder = order;
                    Collections.sort( result, strippedOrder );
                    return (Collection<UIComponent>)result;
                })
                .orElse( composite.components.value() );
    }

}