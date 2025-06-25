/*
 * Copyright (C) 2019-2022, the @authors. All rights reserved.
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

import areca.ui.component2.Property.ReadWrite;

/**
 *
 * @author falko
 */
public class Button
        extends UIComposite {

    public enum Type {
        /** Submit model changes or any other action that changes the persistent store. */
        SUBMIT,
        /** Chnages things, maybe in the model - but does not submit yet. */
        ACTION,
        /** Open a new page or dialog. */
        NAVIGATE
    }

    public enum Format {
        PLAIN, HTML
    }

    public enum IconStyle {
        FILLED, OUTLINED, TWOTONE, ROUND
    }

    public ReadWrite<Button,String> label = new ReadWrite<>( this, "label" );

    public ReadWrite<Button,Type> type = Property.rw( this, "type", Type.ACTION );

    /**
     * The format of the {@link #label}. Default: {@link Format#PLAIN}
     */
    public ReadWrite<Button,Format> format = Property.rw( this, "format", Format.PLAIN );

    /**
     * A ligature or numeric character reference of a
     * <a href="https://fonts.google.com/icons">Material Icon</a>.
     */
    public ReadWrite<Button,String> icon = new ReadWrite<>( this, "icon" );

    /**
     * The style of the {@link #icon}. Default: {@link IconStyle#FILLED}
     */
    public ReadWrite<Button,IconStyle> iconStyle = new ReadWrite<>( this, "iconStyle", IconStyle.FILLED );

    /**
     * Base64 encoded image.
     */
    public ReadWrite<Button,String> image = new ReadWrite<>( this, "imageData" );

    {
        bordered.set( true );
    }

    @Override
    public int computeMinHeight( int width ) {
        return layout.opt().map( l -> l.computeMinHeight( this, width ) ).orElse( UIComponent.DEFAULT_HEIGHT+5 );
    }

    @Override
    public int computeMinWidth( int height ) {
        return layout.opt().map( l -> l.computeMinWidth( this, height ) ).orElse( 50 );
    }

}
