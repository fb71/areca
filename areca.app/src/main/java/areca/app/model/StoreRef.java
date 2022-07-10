/*
 * Copyright (C) 2022, the @authors. All rights reserved.
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
package areca.app.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import areca.common.base.Opt;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 */
public abstract class StoreRef {

    private static final Log LOG = LogFactory.getLog( StoreRef.class );

    private static final char DELIM = '\0';


    @SuppressWarnings("deprecation")
    public static <R extends StoreRef> Opt<R> decode( Class<R> type, String encoded ) {
        try {
            LOG.debug( "Type: %s", type );
            var instance = type.newInstance();

            var split = StringUtils.split( encoded, DELIM );
            if (split[0].equals( instance.prefix() )) {
                for (int i = 1; i < split.length; i++) {
                    instance.parts.add( split[i] );
                }
                return Opt.of( instance );
            }
            else {
                return Opt.absent();
            }
        }
        catch (Exception e) {
            throw (RuntimeException)e;
        }
    }


    // instance *******************************************

    protected List<String> parts = new ArrayList<>();

    /** Decode */
    public StoreRef() {}


    /** */
    public abstract String prefix();


    public String encoded() {
        return prefix() + DELIM + String.join( String.valueOf( DELIM ), parts );
    }


    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * 1 + (parts == null ? 0 : parts.hashCode());
        result = 31 * result + (prefix() == null ? 0 : prefix().hashCode());
        return result;
    }


    @Override
    public boolean equals( Object obj ) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof StoreRef) {
            StoreRef other = (StoreRef)obj;
            return Objects.equals( prefix(), other.prefix() )
                    && Objects.equals( parts, other.parts );
        }
        return false;
    }


    public String toString() {
        return String.format( "%s:%s", prefix(), Arrays.asList( parts ) );
    }
}
