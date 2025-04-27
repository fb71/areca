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
package areca.rt.server.servlet;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.Property.PropertyChangedEvent;
import areca.ui.component2.UIComponentEvent;
import areca.ui.component2.UIComponentEvent.ComponentAttachedEvent;
import areca.ui.component2.UIComponentEvent.ComponentConstructedEvent;
import areca.ui.component2.UIComponentEvent.ComponentConstructingEvent;
import areca.ui.component2.UIComponentEvent.ComponentDetachedEvent;
import areca.ui.component2.UIComponentEvent.ComponentDisposedEvent;
import areca.ui.component2.UIComponentEvent.DecoratorAttachedEvent;
import areca.ui.component2.UIComponentEvent.DecoratorDetachedEvent;

/**
 * The names of {@link UIComponentEvent}s for encoding/decoding.
 *
 * @implNote No table, no enum - just constants for speed.
 * @author Falko Bräutigam
 */
public class EventTypeNames {

    private static final Log LOG = LogFactory.getLog( EventTypeNames.class );

    public static final String COMPONENT_CONSTRUCTING_EVENT = ComponentConstructingEvent.class.getSimpleName();
    public static final String COMPONENT_CONSTRUCTED_EVENT = ComponentConstructedEvent.class.getSimpleName();
    public static final String COMPONENT_ATTACHED_EVENT = ComponentAttachedEvent.class.getSimpleName();
    public static final String COMPONENT_DETACHED_EVENT = ComponentDetachedEvent.class.getSimpleName();
    public static final String COMPONENT_DISPOSED_EVENT = ComponentDisposedEvent.class.getSimpleName();
    public static final String DECORATOR_ATTACHED_EVENT = DecoratorAttachedEvent.class.getSimpleName();
    public static final String DECORATOR_DETACHED_EVENT = DecoratorDetachedEvent.class.getSimpleName();
    public static final String PROPERTY_CHANGED_EVENT = PropertyChangedEvent.class.getSimpleName();

}
