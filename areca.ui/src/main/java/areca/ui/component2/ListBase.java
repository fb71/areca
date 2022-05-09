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
package areca.ui.component2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import areca.common.Assert;
import areca.common.base.Sequence;
import areca.ui.layout.RowConstraints;

/**
 *
 * @param <T> The type of the data elements in this list.
 * @author Falko Bräutigam
 */
public abstract class ListBase<T>
        extends UIComposite {

    private static final Logger LOG = Logger.getLogger( ListBase.class.getName() );

    protected ArrayList<T>              dataArray = new ArrayList<T>();

    protected Map<Integer,UIComposite>  rows = new HashMap<>();


//    @Override
//    protected HtmlNode init( UIComposite newParent ) {
//        layout.set( new RowLayout() );
//        return super.init( newParent );
//    }


    public ListBase<T> setData( int start, Iterable<T> data ) {
        Assert.that( start == 0, "Fragmented data array is not yet supported." );
        dataArray.clear();
        dataArray.addAll( Sequence.of( data ).asCollection() );
        updateRows();
        return this;
    }


    public ListBase<T> setData( int start, @SuppressWarnings("unchecked") T... data ) {
        return setData( start, Arrays.asList( data ) );
    }


    protected void updateRows() {
        Assert.that( rows.isEmpty(), "Updating/adding data is not yet..." );
        for (T data : dataArray) {
            rows.put( rowKey( data ), add( new ListCell(), composite -> {
                composite.layoutConstraints.set( new RowConstraints() {{height.set( 58 );}} );
                initRow( data, composite );
            }));
        }
    }


    private Integer rowKey( T data ) {
        return Integer.valueOf( System.identityHashCode( data ) );
    }


    protected abstract void initRow( T data, UIComposite composite );

    protected abstract void updateRow( T data, UIComposite composite );


    /**
     * Just for CSS styling.
     */
    protected class ListCell
            extends UIComposite {

    }
}
