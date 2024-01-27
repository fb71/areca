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
package areca.ui.viewer.model;

import areca.common.base.Supplier.RSupplier;
import areca.common.event.EventManager;
import areca.common.event.EventManager.EventHandlerInfo;

/**
 *
 * @author Falko Bräutigam
 */
public interface ModelBase {

    public static final ValidationResult VALID = null;

    /**
     * ???
     */
    public class ValidationResult {

        public ValidationResult( NumberFormatException e ) {
        }
    }


    public default EventHandlerInfo subscribe( Object annotated ) {
        return EventManager.instance().subscribe( annotated )
                .performIf( ModelChangeEvent.class, ev -> ev.getSource() == ModelBase.this );
                //.unsubscribeIf( () -> isDisposed() );
    }


    /**
     * Subscribes the given listener to receive a {@link ModelChangeEvent} when the
     * model changes.
     * <p/>
     * The caller has to make sure to add a proper
     * {@link EventHandlerInfo#unsubscribeIf(RSupplier)} clause.
     */
    public default EventHandlerInfo subscribe( ModelChangeListener l ) {
        return EventManager.instance().subscribe( l )
                .performIf( ModelChangeEvent.class, ev -> ev.getSource() == ModelBase.this );
                //.unsubscribeIf( () -> isDisposed() );
    }

}
