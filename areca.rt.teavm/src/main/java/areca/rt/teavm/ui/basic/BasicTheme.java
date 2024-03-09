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
package areca.rt.teavm.ui.basic;

import java.util.HashSet;
import java.util.Set;

import areca.common.event.EventHandler;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.ui.component2.Button;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComponentEvent;
import areca.ui.component2.UIComponentEvent.ComponentConstructedEvent;

/**
 * Basic Areca theme: adds the simple class name of an {@link UIComponent} and the
 * names of all super classes to {@link UIComponent#cssClasses}.
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class BasicTheme {

    private static final Log LOG = LogFactory.getLog( BasicTheme.class );

    public static final ClassInfo<BasicTheme> TYPE = BasicThemeClassInfo.instance();

    /**
     *
     */
    public static void start() {
        BasicTheme theme = new BasicTheme();
        UIComponentEvent.manager().subscribe( theme )
                .performIf( ev -> ev instanceof ComponentConstructedEvent );
    }


    @EventHandler( ComponentConstructedEvent.class )
    public void elementCreaded( ComponentConstructedEvent ev ) {
        if (ev.getSource() instanceof UIComponent) {
            componentCreated( (UIComponent)ev.getSource() );
        }
    }


    protected void componentCreated( UIComponent c ) {
        // init CSS classes
        Set<String> classes = new HashSet<>();
        var cl = (Class<?>)c.getClass();
        for (; !cl.equals( Object.class ); cl = cl.getSuperclass()) {
            if (!cl.getSimpleName().isEmpty()) {
                classes.add( cl.getSimpleName() );
            }
        }
        c.cssClasses.setThemeClasses( classes );

        // bordered via CSS
//        c.bordered.defaultsTo( () -> {
//            return c.cssClasses.values().anyMatches( v -> v.equals( "Bordered" ) );
//        });
        c.bordered.onInitAndChange( (newValue, oldValue) -> {
            c.cssClasses.modify( "Bordered", newValue );
        });

        // ButtonType via CSS
        if (c instanceof Button) {
            ((Button)c).type.onInitAndChange( (newValue, oldValue) -> {
                c.cssClasses.remove( Button.Type.SUBMIT.toString() );
                c.cssClasses.remove( Button.Type.ACTION.toString() );
                c.cssClasses.remove( Button.Type.NAVIGATE.toString() );
                c.cssClasses.add( newValue.toString() );
            });
        }
    }

}
