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
package areca.common;

import areca.common.base.Opt;
import areca.common.base.Supplier.RSupplier;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * Wait for a given condition to be met. Acts more like a lazily init variable that
 * caches its result.
 */
public class WaitFor<T>
        extends Promise.Completable<T> {

    static final Log LOG = LogFactory.getLog( WaitFor.class );

    protected RSupplier<Boolean>    condition;

    protected RSupplier<Throwable>  errorCondition = () -> null;

    protected RSupplier<T>          supplier;


    public WaitFor( RSupplier<Boolean> condition ) {
        this.condition = condition;
    }

    @SuppressWarnings("hiding")
    public WaitFor<T> thenSupply( RSupplier<T> supplier ) {
        this.supplier = supplier;
        return this;
    }

    @SuppressWarnings("hiding")
    public WaitFor<T> errorWhen( RSupplier<Throwable> errorCondition ) {
        this.errorCondition = errorCondition;
        return this;
    }


    @SuppressWarnings("hiding")
    public WaitFor<T> start() {
        Assert.notNull( supplier );
        var error = errorCondition.get();
        if (error != null) {
            LOG.debug( "WAITING: error" );
            completeWithError( error );
        }
        else if (!condition.get()) {
            LOG.debug( "WAITING: ..." );
            Platform.schedule( 100, () -> start() );
        }
        else {
            LOG.debug( "WAITING: done." );
            T value = supplier.supply();

            // if the condition matches in the first run,
            // then give the caller time to register its onSuccess handlers
            Platform.async( () -> complete( value ) );

            // XXX Hack: for ImapSettingsPage
            waitForResult = value;
        }
        return this;
    }


    public Opt<T> waitForResult() {
        return Opt.of( waitForResult );
    }
}