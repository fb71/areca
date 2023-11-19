package areca.common;

import java.util.HashMap;
import java.util.Map;
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

    public static Session current() {
        return SessionScoper.instance.currentSession();
    }

    /**
     * The instance of the given type (for which a factory was previously
     * {@link #registerFactory(Class, RSupplier) registered}) in the scope of the
     * {@link #current()} Session.
     */
    public static <R> R instanceOf( Class<R> type ) {
        return current()._instanceOf( type );
    }


    // instance *******************************************

    // TeaVM does not have Concurrent; should be ok because new instances
    // should happen during init in one thread
    private Map<Class<?>,Object>    instances = new /*Concurrent*/HashMap<>();

    @SuppressWarnings("unchecked")
    public <R> R _instanceOf( Class<R> type ) {
        return (R)instances.computeIfAbsent( type, __ -> {
            try {
                return factories.get( type ).supply();
            }
            catch (Exception e) {
                throw new RuntimeException( "Unable to instantiate session instance of type: " + type.getName(), e );
            }
        });
    }
}
