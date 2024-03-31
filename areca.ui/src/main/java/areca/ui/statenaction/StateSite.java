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

import areca.common.base.Opt;

/**
 * The interface for the {@link State} to communicate with the system.
 *
 * @author Falko Bräutigam
 */
public interface StateSite {

    public StateBuilder createState( Object newState );

    /**
     * Deactivate and dispose this State.
     * <p/>
     * XXX Should be done automatically when {@link State.Dispose} is called?
     */
    public void dispose();

    public boolean isDisposed();

    /**
     * The context variable of the given type and scope.
     */
    public <R> Opt<R> opt( Class<R> type, String scope );

    /**
     * The context variable of the given type and {@link State.Context#DEFAULT_SCOPE}.
     */
    public default <R> Opt<R> opt( Class<R> type ) {
        return opt( type, State.Context.DEFAULT_SCOPE );
    }

    /**
     * The context variable of the given type and scope.
     * @throws NoSuchElementException
     */
    public default <R> R get( Class<R> type, String scope ) {
        return opt( type, scope ).orElseError();
    }

    /**
     * The context variable of the given type and {@link State.Context#DEFAULT_SCOPE}.
     * @throws NoSuchElementException
     */
    public default <R> R get( Class<R> type ) {
        return opt( type, State.Context.DEFAULT_SCOPE ).orElseError();
    }

}
