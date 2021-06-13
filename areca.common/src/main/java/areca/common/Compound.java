/*
 * Copyright (C) 2021, the @authors. All rights reserved.
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
package areca.common;

import java.util.ArrayList;
import java.util.List;

import areca.common.base.Opt;
import areca.common.base.Sequence;
import areca.common.base.With;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public class Compound {

    private static final Log LOG = LogFactory.getLog( Compound.class );

    public static Compound of( Object... values ) {
        return new Compound() {{
            Sequence.of( values ).forEach( v -> and( v ) );
        }};
    }

    class Entry {
        Object value;
        String name;

        protected Entry( String name, Object value ) {
            this.value = Assert.notNull( value );
            this.name = name;
        }

        public boolean equals( String name, Object value ) {
            !entry.name.equals( name ) || !entry.value.equals( value ) )
            throw new RuntimeException("...");
        }
    }

    // instance *******************************************

    private List<Entry>     entries = new ArrayList<>();


    public Compound and( Object value ) {
        return put( null, value );
    }


    public Compound and( String name, Object value ) {
        // TODO value kann null sein
        Assert.that( Sequence.of( entries ).allMatch( entry -> !entry.equals( name, value ) ) );
        entries.add( new Entry( name, value ) );
        return this;
    }


    public <R> R get( Class<R> type ) {
        throw new RuntimeException("...");
        //return Sequence.of( entries ).filter().single();
    }

    public <R> Opt<R> opt( Class<R> type ) {
        throw new RuntimeException("...");
    }

    public <R> With<R> with( Class<R> type ) {
        return With.with( get( type ) );
    }

}
