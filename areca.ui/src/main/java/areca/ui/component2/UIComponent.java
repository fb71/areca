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
package areca.ui.component2;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;

import areca.common.Assert;
import areca.common.base.Opt;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Color;
import areca.ui.Position;
import areca.ui.Size;
import areca.ui.component2.Property.ReadOnly;
import areca.ui.component2.Property.ReadWrite;
import areca.ui.component2.Property.ReadWrites;
import areca.ui.layout.LayoutConstraints;
import areca.ui.layout.LayoutManager;

/**
 *
 * @author falko
 */
public abstract class UIComponent {

    private static final Log LOG = LogFactory.getLog( UIComponent.class );

    public static final String          PROP_CSS_CLASSES = "cssClasses";
    public static final String          PROP_BG_COLOR = "bgColor";
    public static final String          PROP_SIZE = "size";
    public static final String          PROP_POSITION = "position";

    private static volatile int         ID_COUNT;

    private int                         id = ID_COUNT++;

    private boolean                     disposed;

    private UIComposite                 parent;

    private Map<String,Object>          data = new TreeMap<>();

    public Object                       htmlElm;

    /** The styling classes of this component. */
    public ReadWrites<UIComponent,String>   cssClasses = Property.rws( this, PROP_CSS_CLASSES );

    /** Background color. */
    public ReadWrite<UIComponent,Color>     bgColor = Property.rw( this, PROP_BG_COLOR );

    /** The size of the component. Usually this is set by a {@link LayoutManager} only. */
    public ReadWrite<UIComponent,Size>      size = Property.rw( this, PROP_SIZE );

    public ReadOnly<UIComponent,Size>       clientSize = Property.rw( this, "clientSize" );

    /** The position of the component. Usually this is set by a {@link LayoutManager} only. */
    public ReadWrite<UIComponent,Position>  position = Property.rw( this, PROP_POSITION );

    /** Implemented via CSS class "Bordered". */
    public ReadWrite<UIComponent,Boolean>   bordered = Property.rw( this, "bordered" );

    /**  */
    public ReadWrite<UIComponent,Float>     opacity = Property.rw( this, "opacity" );

    /** */
    public ReadWrite<UIComponent,LayoutConstraints> layoutConstraints = Property.rw( this, "lc" );

    /** */
    public Events events = new Events( this );


    // methods ********************************************

    {
        // init CSS classes
        Set<String> classes = new HashSet<>();
        for (Class<?> cl=getClass(); !cl.equals( Object.class ); cl=cl.getSuperclass()) {
            if (!cl.getSimpleName().isEmpty()) {
                classes.add( cl.getSimpleName() );
            }
        }
        cssClasses.set( classes );

        clientSize = new ReadOnly<UIComponent,Size>( this, "clientSize" ) {
            @Override
            public Opt<Size> opt() {
                return size.opt().ifPresentMap( s -> s.substract( 5, 5 ) );
            }
        };

//        .defaultsTo( () -> {
//            clientSize.valuePresent = false;
//            var currentSize = size.value();
//            return Size.of( currentSize.width()-10, currentSize.height()-10 );
//        });

        bordered.defaultsTo( () -> {
            return cssClasses.values().anyMatches( v -> v.equals( "Bordered" ) );
        });
        bordered.onChange( ( newValue, oldValue ) -> {
            if (newValue) {
                cssClasses.add( "Bordered" );
            } else {
                cssClasses.remove( "Bordered" );
            }
        });
    }


    protected UIComponent() {
        UIComponentEvent.manager.publish( new UIComponentEvent.ComponentConstructedEvent( this ) );
    }


    /**
     * Called when this component is added to the component hierarchy.
     */
    protected void attachedTo( UIComposite newParent ) {
        Assert.isNull( parent, "parent should not be set before init()" );
        this.parent = newParent;
        UIComponentEvent.manager.publish( new UIComponentEvent.ComponentAttachedEvent( this ) );
    }


    /**
     * Called when this component is added to the component hierarchy.
     */
    protected void detachedFrom( @SuppressWarnings("hiding") UIComposite parent ) {
        Assert.that( this.parent == parent, "no parent" );
        this.parent = null;
        UIComponentEvent.manager.publish( new UIComponentEvent.ComponentDetachedEvent( this ) );
    }


    /**
     * Destruct this component. If this component is attached to a parent it is
     * automatically removed.
     */
    public void dispose() {
        Assert.that( !isDisposed() );
        disposed = true;
        //htmlElm = null;
        events.dispose();
        if (parent != null) {
            parent.components.remove( this );
        }
        UIComponentEvent.manager.publish( new UIComponentEvent.ComponentDisposedEvent( this ) );
    }


    public boolean isDisposed() {
        return disposed;
    }


    public int id() {
        return id;
    }


    public UIComposite parent() {
        return parent;
    }


    @SuppressWarnings("unchecked")
    public <R extends LayoutConstraints> Opt<R> layoutConstraints() {
        return (Opt<R>)layoutConstraints.opt();
    }


    public int computeMinWidth( int height ) {
        return 50;
    }


    public int computeMinHeight( int width ) {
        return 50;
    }


    @SuppressWarnings("unchecked")
    public <R> R data( String name, Supplier<R> initializer ) {
        return (R)data.computeIfAbsent( name, key -> {
            return initializer.get();
        });
    }

    @SuppressWarnings("unchecked")
    public <R> Opt<R> optData( String name ) {
        return Opt.of( (R)data.get( name ) );
    }

}
