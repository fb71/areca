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
package areca.systemservice.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import areca.common.Assert;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * Immutable list of {@link String} parts, separated by a {@link #DELIMITER}.
 *
 * @author Falko Bräutigam
 */
public class Path
        implements Iterable<String> {

    private static final Log log = LogFactory.getLog( Path.class );

    public static final String  DELIMITER = "/";


    public static Path parse( String pathString ) {
        return new Path( pathString );
    }


    // instance *******************************************

    private List<String>        parts;


    protected Path( String pathString ) {
        parts = Arrays.asList( StringUtils.split( pathString, DELIMITER ) );
    }


    protected Path( List<String> parts ) {
        this.parts = new ArrayList<>();  // ensure initial capacity
        this.parts.addAll( parts );
    }


    @Override
    public Iterator<String> iterator() {
        return parts.iterator();
    }


    public String toString() {
        return String.join( DELIMITER, parts );
    }


    public Path plusPath( String pathString ) {
        return Sequence.of( parse( pathString ) ).reduce( this, Path::plus );
    }


    public Path plus( String part ) {
        Assert.that( !part.contains( DELIMITER ) );
        Path result = new Path( parts );
        result.parts.add( part );
        return result;
    }


    public Path stripFirst( int i ) {
        Assert.that( i > 0 && i <= length() );
        return new Path( parts.subList( i, parts.size() ) );
    }


    public int length() {
        return parts.size();
    }


    public String part( int index ) {
        return parts.get( index );
    }


    public String lastPart() {
        return part( length()-1 );
    }

}
