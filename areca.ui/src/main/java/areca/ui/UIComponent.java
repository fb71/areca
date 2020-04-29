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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.logging.Logger;

import areca.common.base.Opt;
import areca.common.event.EventListener;
import areca.common.event.EventManager;
import areca.common.event.EventManager.EventHandlerInfo;
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
        layoutConstraints.set( new ArrayList<>() );
    }


    public void dispose() {
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


    public EventHandlerInfo subscribe( EventListener<?> l ) {
        return EventManager.instance().subscribe( l )
                .performIf( ev -> ev != null && ev.getSource() == UIComponent.this);
    }


    @SuppressWarnings("unchecked")
    public <R> R data( String name, Supplier<R> initializer ) {
        return (R)data.computeIfAbsent( name, key -> {
            return initializer.get();
        });
    }

    @SuppressWarnings("unchecked")
    public <R> Opt<R> optData( String name ) {
        return Opt.ofNullable( (R)data.get( name ) );
    }


//    public <C extends UIComponent,E extends Exception> C with( Consumer<C,E> task ) throws E {
//        task.perform((C)this);
//        return (C)this;
//    }

    @FunctionalInterface
    public interface Consumer<P,E extends Exception> {

        public void perform(P param) throws E;
    }

}
