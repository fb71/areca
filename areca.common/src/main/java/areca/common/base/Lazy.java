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
package areca.common.base;

import areca.common.Assert;

/**
 *
 * @author Falko Bräutigam
 */
public class Lazy<T,E extends Exception>
        implements Supplier<T,E> {

    /**
     * A {@link Lazy} that does not throw any checked Exception.
     */
    public static class RLazy<T>
            extends Lazy<T,RuntimeException> {

        public RLazy() {
            super();
        }

        public RLazy( Supplier<T,RuntimeException> delegate ) {
            super( delegate );
        }

    }

    // instance *******************************************

    private Supplier<T,E>       delegate;

    private volatile boolean    initialized = false;

    private T                   value;


    public Lazy() {
    }


    public Lazy( Supplier<T,E> delegate ) {
        this.delegate = Assert.notNull( delegate );
    }


    public T $() throws E {
        return supply();
    }

    @Override
    public T supply() throws E {
        return supply( Assert.notNull( delegate, "Lazy was initialized without supplier." ) );
    }


    public T supply( Supplier<T,E> supplier ) throws E {
        if (!initialized) {
            synchronized(this) {
                if (!initialized) {
                    initialized = true;
                    value = supplier.supply();
                }
            }
        }
        return value;
    }
}
