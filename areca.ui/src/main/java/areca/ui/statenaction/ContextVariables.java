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
package areca.ui.statenaction;

import java.util.ArrayList;
import java.util.List;

import areca.common.Assert;
import areca.common.base.Opt;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * Stackable list of values retrievable by type and scope.
 *
 * @author Falko Bräutigam
 */
public class ContextVariables {

    private static final Log LOG = LogFactory.getLog( ContextVariables.class );

    private ContextVariables    parent;

    private List<Scoped>        entries = new ArrayList<>();


    /**
     * Creates a new instance with no parent.
     */
    protected ContextVariables() {}


    public ContextVariables( ContextVariables parent ) {
        this.parent = Assert.notNull( parent );
    }


    public void put( Object value, String scope ) {
        Assert.that( localEntry( value.getClass(), scope ).isAbsent(), "Local entry already exists: " + value + " [" + scope + "]" );
        entries.add( new Scoped( value, scope ) );
    }


    public <R> Opt<R> entry( Class<R> type, String scope ) {
        var result = localEntry( type, scope ).orNull();
        return result != null
                ? Opt.of( result )
                : parent != null ? parent.entry( type, scope ) : Opt.absent();
    }


    public <R> Opt<R> localEntry( Class<R> type, String scope ) {
        return Sequence.of( entries )
                .first( scoped -> scoped.isCompatible( type, scope ) )
                .map( scoped -> type.cast( scoped.value ) );
    }

    /**
     *
     */
    protected static class Scoped {
        String      scope;
        Object      value;

        public Scoped( Object value, String scope ) {
            this.scope = Assert.notNull( scope, "Scope must not be null. Use DEFAULT_SCOPE instead." );
            this.value = Assert.notNull( value, "Value of a context variable must not be null. Removing is not yet supported." );
        }

        public boolean isCompatible( Class<?> type, @SuppressWarnings("hiding") String scope ) {
            return type.isAssignableFrom( value.getClass() ) && scope.equals( this.scope );
        }
    }

}
