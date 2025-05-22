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

import java.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import areca.common.Assert;
import areca.ui.component2.Property.ReadWrite;

/**
 *
 * @author falko
 */
public class Image
        extends UIComponent {

    public static String base64( byte[] bytes ) {
        var encoded = Base64.getEncoder().encode( bytes );
        return new String( encoded, 0, encoded.length, StandardCharsets.ISO_8859_1 );
    }

    /**
     * Base64 encoded image data.
     * @see #setData(InputStream)
     * @see #setData(byte[])
     */
    public ReadWrite<Image,String> data = new ReadWrite<>( this, "data" );

    /** URL that points to the image */
    public ReadWrite<Image,String> src = new ReadWrite<>( this, "src" );

    /**
     * Reads image data into {@link #data} and closes the stream.
     * @throws RuntimeException If {@link IOException} was thrown while reading.
     */
    public Image setData( InputStream in ) {
        try (var closeable = in; var out = new ByteArrayOutputStream( 4096 )) {
            var buf = new byte[4096];
            for (int c = in.read( buf ); c != -1; c = in.read( buf )) {
                out.write( buf, 0, c );
            }
            setData( out.toByteArray() );
            return this;
        }
        catch (IOException e) {
            throw new RuntimeException( e );
        }
    }

    public Image setData( byte[] data ) {
        Assert.that( src.opt().isAbsent() );
        this.data.set( base64( data ) );
        return this;
    }

//    @Override
//    public int computeMinHeight( int width ) {
//        return layout.opt().map( l -> l.computeMinHeight( this, width ) ).orElse( UIComponent.DEFAULT_HEIGHT+5 );
//    }
//
//    @Override
//    public int computeMinWidth( int height ) {
//        return layout.opt().map( l -> l.computeMinWidth( this, height ) ).orElse( 50 );
//    }

}
