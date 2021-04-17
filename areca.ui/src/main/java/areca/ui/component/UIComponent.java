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
package areca.ui.component;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.logging.Logger;

import areca.common.base.Consumer;
import areca.common.base.Opt;
import areca.common.base.Sequence;
import areca.common.event.EventListener;
import areca.common.event.EventManager;
import areca.ui.Color;
import areca.ui.Position;
import areca.ui.Size;
import areca.ui.component.Property.ReadWrite;
import areca.ui.component.Property.ReadWrites;
import areca.ui.html.HtmlElement;
import areca.ui.layout.LayoutConstraints;
import areca.ui.layout.LayoutManager;

/**
 *
 * @author falko
 */
public abstract class UIComponent {

    private static final Logger LOG = Logger.getLogger( UIComponent.class.getSimpleName() );

    //public static final UIComponent         TYPE = new UIComponent() {};

    private static volatile int         ID_COUNT;

    private int                         id = ID_COUNT++;

    private UIComposite                 parent;

    private Map<String,Object>          data = new TreeMap<>();

    protected HtmlElement               htmlElm;

    /**
     * The styling classes of this component.
     */
    protected ReadWrites<String> cssClasses = new ReadWrites<>( this, "cssClasses" ) {
        @Override
        public Sequence<String,RuntimeException> sequence() {
            return Sequence.of( htmlElm.attributes.opt( "class" ).orElse( "" ).split( " " ) );
        }
        @Override
        public void doAdd( String value ) {
            htmlElm.attributes.set( "class", String.join( " ", sequence().concat( value ).toSet() ) );
        }
        @Override
        protected void doRemove( String value ) {
            htmlElm.attributes.set( "class", String.join( " ", sequence().filter( elm -> !elm.equals( value ) ).toSet() ) );
        }
    };

    /**
     * Background color.
     */
    public ReadWrite<Color> bgColor = Property.create( this, "bgcolor",
            () -> htmlElm.styles.color( "background-color").orElse( null ),
            newValue -> htmlElm.styles.set( "background-color", newValue ) );

    /**
     * The size of the component. Usually this is set by a {@link LayoutManager} only.
     */
    public ReadWrite<Size> size = Property.create( this, "size",
            () -> htmlElm.offsetSize.get(),
            newValue -> htmlElm.styles.set( "", newValue ) );

    /**
     * The position of the component. Usually this is set by a {@link LayoutManager} only.
     */
    public ReadWrite<Position> position = Property.create( this, "position",
            () -> htmlElm.offsetPosition.get(),
            newValue -> htmlElm.styles.set( "", newValue ) );

    /**
     *
     */
    public ReadWrite<Boolean> bordered = Property.create( this, "bordered",
            () -> cssClasses.sequence().anyMatches( v -> v.equals( "Bordered" ) ),
            newValue -> { if (newValue) cssClasses.add( "Bordered" ); else cssClasses.remove( "Bordered" ); } );

    /**
     *
     */
    public ReadWrite<LayoutConstraints> layoutConstraints = Property.create( this, "lc" );


    /** Instantiate via {@link UIComposite#create(Class, Consumer...)} only. */
    protected UIComponent() { }


    protected void init( UIComposite newParent ) {
        this.parent = newParent;

        for (Class<?> cl=getClass(); !cl.equals( Object.class ); cl=cl.getSuperclass()) {
            cssClasses.add( cl.getSimpleName() );
        }

        LOG.info( "INIT " + size.get() );
        //EventManager.instance().publish( new UIRenderEvent.ComponentCreatedEvent( this ) );
    }


    public void dispose() {
        throw new RuntimeException( "not yet..." );
        //EventManager.instance().publish( new UIRenderEvent.ComponentCreatedEvent( this ) );
    }


    public int id() {
        return id;
    }


    public UIComposite parent() {
        return parent;
    }


    @SuppressWarnings("unchecked")
    public <R> R layoutConstraints() {
        return (R)layoutConstraints.get();
    }


    public int computeMinimumWidth( int height ) {
        return 50;
    }


    public int computeMinimumHeight( int width ) {
        return 50;
    }


    public void onClick( EventListener<SelectionEvent> l ) {
        htmlElm.listeners.click( htmlEvent -> {
            SelectionEvent ev = new SelectionEvent( UIComponent.this );
            l.handle( ev );
            EventManager.instance().publish( ev );
        });
    }


//    public EventHandlerInfo subscribe( EventListener<?> l ) {
//        return EventManager.instance().subscribe( l )
//                .performIf( ev -> ev != null && ev.getSource() == UIComponent.this);
//    }


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

}
