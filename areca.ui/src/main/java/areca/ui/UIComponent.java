/*
 * Copyright (C) 2019, the @authors. All rights reserved.
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
package areca.ui;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.logging.Logger;

import areca.common.event.EventListener;
import areca.common.event.EventManager;
import areca.ui.layout.LayoutConstraint;

/**
 *
 * @author falko
 */
public abstract class UIComponent {

    private static final Logger LOG = Logger.getLogger( UIComponent.class.getSimpleName() );

    public static final UIComponent         TYPE = new UIComponent() {};

    private static volatile int             ID_COUNT;

    private int                             id = ID_COUNT++;

    private UIComposite                     parent;

    private Map<String,Object>              data = new TreeMap<>();

    public Property<List<LayoutConstraint>> layoutConstraints = Property.create( this, "layoutConstraints" );

    public Property<Color>                  bgColor = Property.create( this, "bgcolor" );

    public Property<Size>                   size = Property.create( this, "size" );

    public Property<Point>                  position = Property.create( this, "position" );


    /** Instantiate via {@link UIComposite#create(Class, Consumer...)} only. */
    protected UIComponent() { }


    protected void init( UIComposite newParent ) {
        this.parent = newParent;
        EventManager.instance().publish( new UIRenderEvent.ComponentCreated( this ) );
        layoutConstraints.set( new ArrayList() );
    }


    public void destroy() {
        throw new RuntimeException( "not yet..." );
        //EventManager.instance().publish( new UIRenderEvent.ComponentCreated( this ) );
    }


    public int id() {
        return id;
    }


    public UIComposite parent() {
        return parent;
    }


    public Size computeMinimumSize( Size toCheck ) {
        return toCheck;
    }


    public <T extends EventObject> void subscribe( EventListener<T> l ) {
        EventManager.instance().subscribe( (EventObject ev) -> {
            if (ev.getSource() == UIComponent.this) {
                l.handle( (T)ev );
            }
        });
    }


    public <R> R getOrCreateData( String name, Supplier<R>... initializer ) {
        assert initializer.length <= 1 : "Too many initializers: " + initializer.length;

        return (R)data.computeIfAbsent( name, key -> {
            if (initializer.length == 0) {
                throw new IllegalStateException( "No data for key: " + name );
            }
            return initializer[0].get();
        });
    }


    public <C extends UIComponent,E extends Exception> C with( Consumer<C,E> task ) throws E {
        task.perform((C)this);
        return (C)this;
    }

    @FunctionalInterface
    public interface Consumer<P,E extends Exception> {

        public void perform(P param) throws E;
    }

}
