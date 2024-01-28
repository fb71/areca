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
package areca.rt.server.servlet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import areca.common.Assert;
import areca.common.event.EventHandler;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.rt.server.servlet.JsonServer2ClientMessage.JsonUIComponentEvent;
import areca.ui.App.RootWindow;
import areca.ui.component2.Property.PropertyChangedEvent;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComponentEvent;
import areca.ui.component2.UIComponentEvent.ComponentAttachedEvent;
import areca.ui.component2.UIComponentEvent.ComponentConstructedEvent;
import areca.ui.component2.UIComponentEvent.ComponentConstructingEvent;
import areca.ui.component2.UIComponentEvent.ComponentDetachedEvent;
import areca.ui.component2.UIComponentEvent.ComponentDisposedEvent;

/**
 * A collector of all UI render events: {@link UIComponentEvent} and
 * {@link PropertyChangedEvent}.
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class UIEventCollector {

    private static final Log LOG = LogFactory.getLog( UIEventCollector.class );

    public static final ClassInfo<UIEventCollector> TYPE = UIEventCollectorClassInfo.instance();

    private Map<Integer,UIComponent>    components = new HashMap<>();

    private List<JsonUIComponentEvent>  events = new ArrayList<>( 512 );


    public void start() {
        UIComponentEvent.manager().subscribe( this );
    }


    public List<JsonUIComponentEvent> events() {
        try { return events; } finally { events = new ArrayList<>( 512 ); }
    }


    public UIComponent componentForId( Integer componentId ) {
        return Assert.notNull( components.get( componentId ), "No such componentId: " + componentId );
    }

    /** The RootWindow component */
    public UIComponent rootWindow() {
        return components.values().stream().filter( c -> c instanceof RootWindow ).findAny().get();
    }


    @EventHandler( PropertyChangedEvent.class )
    public void propertyChanged( PropertyChangedEvent ev ) {
        var prop = ev.getSource();
        if (prop.component() instanceof UIComponent) {
            LOG.debug( "PROPERTY: %s", prop.name() );
            Assert.notNull( components.get( ((UIComponent)prop.component()).id() ) );
            events.add( new JsonUIComponentEvent( ev ) );
        }
        else {
            LOG.debug( "SKIP: %s (%s)", prop.name(), prop.component() );
        }
    }


    @EventHandler( ComponentConstructingEvent.class )
    public void componentConstructing( ComponentConstructingEvent ev ) {
        var component = ev.getSource();
        LOG.debug( "CONSTRUCTING: %s (id=%s)", component.getClass().getName(), component.id() );
        Assert.isNull( components.put( component.id(), component ) );
        events.add( new JsonUIComponentEvent( ev ) );
    }


    @EventHandler( ComponentConstructedEvent.class )
    public void componentConstructed( ComponentConstructedEvent ev ) {
        var component = ev.getSource();
        LOG.debug( "CONSTRUCTED: %s (id=%s)", component.getClass().getName(), component.id() );
    }


    @EventHandler( ComponentAttachedEvent.class )
    public void componentAttached( ComponentAttachedEvent ev ) {
        LOG.debug( "ATTACHED: %s", ev.getSource().getClass().getSimpleName() );
        events.add( new JsonUIComponentEvent( ev ) );
    }


    @EventHandler( ComponentDetachedEvent.class )
    public void componentDetached( ComponentDetachedEvent ev ) {
        LOG.debug( "DETACHED: %s", ev.getSource().getClass().getSimpleName() );
        events.add( new JsonUIComponentEvent( ev ) );
    }


    @EventHandler( ComponentDisposedEvent.class )
    public void componentDisposed( ComponentDisposedEvent ev ) {
        var component = ev.getSource();
        LOG.debug( "DISPOSED: %s", component.getClass().getName() );
        Assert.notNull( components.remove( component.id() ) );
        events.add( new JsonUIComponentEvent( ev ) );
    }

}
