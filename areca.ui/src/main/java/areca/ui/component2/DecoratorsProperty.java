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
package areca.ui.component2;

import java.util.ArrayList;

import areca.common.Assert;
import areca.common.base.Opt;

/**
 * The {@link UIComponent#decorators} property.
 *
 * @author Falko Bräutigam
 */
public class DecoratorsProperty
        extends Property.ReadWrites<UIComponent,UIComponentDecorator> {

    protected DecoratorsProperty( UIComponent component ) {
        super( component, UIComponent.PROP_DECORATORS );
        rawSet( new ArrayList<>() );
    }

    @Override
    public Opt<UIComponentDecorator> add( UIComponentDecorator add ) {
        Assert.that( !TextField.class.isInstance( add ), "Unfortunatelly Label does not work directly with TextField :(" );
        return super.add( Assert.notNull( add ) ).ifPresent( __ -> {
            add.decoratorAttachedTo( component );
        });
    }

    @Override
    public Opt<UIComponentDecorator> remove( UIComponentDecorator remove ) {
        return super.remove( remove ).ifPresent( __ -> {
            remove.decoratorDetachedFrom( component );
        });
    }

    public void disposeAll() {
        new ArrayList<>( value ).forEach( d -> d.dispose() );
        Assert.isEqual( 0, size(), "Number of decorators after disposeAll(): " + size() );
    }

    public int size() {
        return value.size();
    }
}