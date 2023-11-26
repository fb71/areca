package areca.common;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import areca.common.base.Supplier;
import areca.common.base.Supplier.RSupplier;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/*
 * Copyright (C) 2023, the @authors. All rights reserved.
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

/**
 *
 * @author Falko Bräutigam
 */
public class Session {

    private static final Log LOG = LogFactory.getLog( Session.class );

    private static final Map<Class<?>,Supplier<?,Exception>> factories = new HashMap<>();


    /**
     * Registers a factory of session scoped variable of the given type.
     */
    public static <T> void registerFactory( Class<T> type, Supplier<T,Exception> supplier ) {
        Assert.isNull( factories.put( type, supplier ) );
    }


    @SuppressWarnings("unchecked")
    public static <R> R createInstance( Class<R> type ) {
        try {
            return (R)Assert.notNull( factories.get( type ), "No factory registered." ).supply();
        }
        catch (Exception e) {
            throw new RuntimeException( "Unable to instantiate session instance of type: " + type.getName(), e );
        }
    }


    public static Session current() {
        return SessionScoper.instance.currentSession();
    }

    /**
     * The instance of the given type (for which a factory was previously
     * {@link #registerFactory(Class, RSupplier) registered}) in the scope of the
     * {@link #current()} Session.
     *
     * @throws AssertionException If no instance could be found and/or created via factory.
     */
    public static <R> R instanceOf( Class<R> type ) {
        return current()._instanceOf( type );
    }


    /**
     *
     * @throws AssertionException If an instance with this type is already there.
     * @return instance
     */
    public static <R> R setInstance( R instance ) {
        return current()._setInstance( instance );
    }


    // instance *******************************************

    // TeaVM does not have Concurrent; should be ok because new instances
    // should happen during init in one thread
    private Map<Class<?>,Object>    instances = new /*Concurrent*/HashMap<>();

    /**
     * Cache of last result.
     * <p>
     * Not volatile, not synchronized: we read/use/modify our CPU's cache version.
     */
    private Pair<Class<?>,Object>   last;


    public void dispose() {
        instances.clear();
        last = null;
    }


    private <R> R _setInstance( R instance ) {
        Assert.isNull( instances.put( instance.getClass(), instance ) );
        return instance;
    }


    @SuppressWarnings("unchecked")
    public <R> R _instanceOf( Class<R> type ) {
        var lastStable = last;
        if (lastStable != null && lastStable.getKey() == type) {
            return (R)lastStable.getValue();
        }
        var result = (R)instances.computeIfAbsent( type, __ -> createInstance( type ) );
        last = ImmutablePair.of( type, result );
        return result;
    }
}
