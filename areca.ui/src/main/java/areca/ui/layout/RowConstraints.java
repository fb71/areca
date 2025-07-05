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
package areca.ui.layout;

import areca.ui.Orientation;
import areca.ui.component2.Property;
import areca.ui.component2.Property.ReadWrite;
import areca.ui.component2.UIComponent;

/**
 *
 * @author Falko Bräutigam
 */
public class RowConstraints
        implements LayoutConstraints {

    /**
     * See {@link #width}.
     */
    public static RowConstraints width( int width ) {
        return new RowConstraints().width.set( width );
    }

    /**
     * See {@link #height}.
     */
    public static RowConstraints height( int height ) {
        return new RowConstraints().height.set( height );
    }

    /**
     * See {@link #height} and {@link #heightPercent}.
     */
    public static RowConstraints height( int height, int heightPercent ) {
        return new RowConstraints().height.set( height ).heightPercent.set( heightPercent );
    }

    public static RowConstraints size( int width, int height ) {
        return new RowConstraints().height.set( height ).width.set( width );
    }

    /**
     * {@link Orientation#VERTICAL}:
     * <ul>
     * <li><b>absolute</b> height of the component</li>
     * <li><b>minimum</b> height if {@link #heightPercent} is given</li>
     * </ul>
     * If neither is set:
     * <p><ul>
     * <li>RowLayout#fillHeight == true: the components fill the available space equally
     * <li>RowLayout#fillHeight == false: {@link UIComponent#minimumHeight} is used
     * </ul>
     */
    public ReadWrite<RowConstraints,Integer>       height = Property.rw( this, "height" );

    /**
     * {@link Orientation#HORIZONTAL}: The *absolute* width of the component.
     */
    public ReadWrite<RowConstraints,Integer>       width = Property.rw( this, "width" );

    /**
     * The percentage height of the component (of the entire available client size of
     * the parent). The lower limit is the absolute height given by {@link #height}.
     */
    public ReadWrite<RowConstraints,Integer>       heightPercent = Property.rw( this, "heightPercent" );

    //public ReadWrite<RowConstraints,Integer>       widthPercent = Property.rw( this, "widthPercent" );

}
