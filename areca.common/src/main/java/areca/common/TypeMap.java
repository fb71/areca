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
package areca.common;

import java.util.HashMap;

import areca.common.base.Opt;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * Mainly for propagating results of {@link Promise} chains.
 *
 * @author Falko Bräutigam
 */
public class TypeMap
        extends HashMap<Class<?>,Object> {

    private static final Log LOG = LogFactory.getLog( TypeMap.class );

    public <R> R put( R instance ) {
        var prev = put( instance.getClass(), instance );
        Assert.isNull( prev, "Already added: " + instance.getClass() );
        return instance;
    }

    @SuppressWarnings( "unchecked" )
    public <R> Opt<R> opt( Class<R> type ) {
        return Opt.of( (R)super.get( type ) );
    }

    public <R> R get( Class<R> type ) {
        return opt( type ).orElseError();
    }
}
