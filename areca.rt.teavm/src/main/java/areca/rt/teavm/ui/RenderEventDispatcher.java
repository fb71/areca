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
package areca.rt.teavm.ui;

import java.util.HashMap;
import java.util.Map;

import java.lang.reflect.InvocationTargetException;

import areca.common.Assert;
import areca.common.base.Sequence;
import areca.common.event.EventHandler;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.MethodInfo;
import areca.common.reflect.RuntimeInfo;
import areca.ui.component2.Property;
import areca.ui.component2.Property.PropertyChangedEvent;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class RenderEventDispatcher {

    private static final Log LOG = LogFactory.getLog( RenderEventDispatcher.class );

//    static final Map<Class,?> RENDERERS = new HashMap<>() {{
//        put( UIComposite.class, new UICompositeRenderer() );
//    }};
//
//
//    @EventHandler( ComponentCreatedEvent.class )
//    public void componentCreated( ComponentCreatedEvent ev ) throws InvocationTargetException {
//        UIComponent component = ev.getSource();
//        var renderer = rendererFor( component );
//        var m = Sequence.of( ClassInfo.of( renderer ).methods() )
//                .filter( _m -> _m.name().equals( "componentCreated" ) )
//                .single();
//        m.invoke( renderer );
//    }
//
//
//    @EventHandler( ComponentDisposedEvent.class )
//    public void componentDisposed( ComponentCreatedEvent ev ) {
//        throw new RuntimeException( "not yet ..." );
//
//    }
//
//
//    @EventHandler( Property.PropertyChangedEvent.class )
//    public void propertyChanged( PropertyChangedEvent ev ) throws InvocationTargetException {
//        Property<?,?> p = ev.getSource();
//        UIComponent component = (UIComponent)p.component();
//
//        var renderer = rendererFor( component );
//        var ci = ClassInfo.of( renderer );
//        for (MethodInfo m : ci.methods()) {
//            m.annotation( Render.class ).ifPresent( a -> {
//                m.invoke( renderer, component, component.htmlElm, ev.getNewValue(), ev.getOldValue() );
//            });
//        }
//    }
//
//
//    protected Object rendererFor( UIComponent component ) {
//        return Assert.notNull( RENDERERS.get( component.getClass() ),
//                "No renderer for component type: ", component.getClass().getSimpleName() );
//    }

}
