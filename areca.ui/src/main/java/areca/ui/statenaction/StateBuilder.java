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

/**
 *
 * @author Falko Bräutigam
 */
public interface StateBuilder {

    StateBuilder putContext( Object value, String scope );

    /**
     * Actually activates the newly created {@link State}.
     */
    void activate();

    /**
     * Register a listener for {@link StateChangeEvent State lifecycle events}. The
     * listener is unsubscribed when the State is disposed.
     *
     * @param annotatedOrListener {@link StateChangeListener} or an annotated class.
     */
    StateBuilder onChange( Object annotatedOrListener );

    default StateBuilder onChange( StateChangeListener l ) {
        return onChange( (Object)l );
    }

}
