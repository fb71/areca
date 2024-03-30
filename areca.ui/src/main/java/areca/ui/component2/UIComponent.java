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
import java.util.TreeMap;
import java.util.function.Supplier;

import areca.common.Assert;
import areca.common.base.Function;
import areca.common.base.Function.RFunction;
import areca.common.base.Opt;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Align;
import areca.ui.Color;
import areca.ui.Position;
import areca.ui.Size;
import areca.ui.component2.Property.ReadOnly;
import areca.ui.component2.Property.ReadWrite;
import areca.ui.component2.Property.ReadWrites;
import areca.ui.component2.UIComponentEvent.ComponentAttachedEvent;
import areca.ui.component2.UIComponentEvent.ComponentConstructedEvent;
import areca.ui.component2.UIComponentEvent.ComponentDetachedEvent;
import areca.ui.layout.LayoutConstraints;
import areca.ui.layout.LayoutManager;

/**
 *
 * @author falko
 */
public abstract class UIComponent
        extends UIElement {

    private static final Log LOG = LogFactory.getLog( UIComponent.class );

    /** Default height of standard components, like {@link TextField}, {@link Button}, etc. */
    public static final int             DEFAULT_HEIGHT = 30;

    public static final String          PROP_CSS_CLASSES = "cssClasses";
    public static final String          PROP_CSS_STYLES = "cssStyles";
    public static final String          PROP_BG_COLOR = "bgColor";
    public static final String          PROP_SIZE = "size";
    public static final String          PROP_POSITION = "position";
    public static final String          PROP_EVENTS = "events";
    public static final String          PROP_DECORATORS = "decorators";

    private UIComposite                 parent;

    private Map<String,Object>          data;

    public Object                       htmlElm;

    /**
     * The tooltip of this component.
     */
    public ReadWrite<UIComponent,String> tooltip = Property.rw( this, "tooltip" );

    /**
     * The CSS styling classes of this component.
     */
    public CssClassesProperty           cssClasses = new CssClassesProperty( this );

    /**
     * Explicite CSS styling of this component.
     * <p>
     * Prefer using {@link #cssClasses} instead!
     * @see UIComponent#cssClasses
     */
    public ReadWrites<UIComponent,CssStyle> styles = Property.rws( this, PROP_CSS_STYLES, new HashSet<>() );

    /** Value of {@link UIComponent#styles} */
    public static class CssStyle {

        public static CssStyle of( String name, String value ) {
            return new CssStyle( name, value );
        }

        public String name;
        public String value;

        public CssStyle( String name, String value ) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString() {
            return String.format( "[%s = %s]", name, value );
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public boolean equals( Object obj ) {
            return obj instanceof CssStyle ? name.equals( ((CssStyle)obj).name ) : false;
        }
    }

    /**
     * Background color.
     */
    public ReadWrite<UIComponent,Color>     bgColor = Property.rw( this, PROP_BG_COLOR );

    /**
     * Base64 encoded image data to be displayed as the background of this component.
     */
    public ReadWrite<UIComponent,String>    bgImage = new ReadWrite<>( this, "bgImage" );

    /**
     * The size of the component. Usually this is set by a {@link LayoutManager} only.
     * */
    public ReadWrite<UIComponent,Size>      size = Property.rw( this, PROP_SIZE );

    public ReadOnly<UIComponent,Size>       clientSize = Property.rw( this, "clientSize" );

    /**
     * A {@link Function} that is set bei the renderer to give client code (layouter) access
     * to the actual minimum size of the components backend HTML element.
     * <p>
     * <b>EXPERIMENTAL</b>: often does not work as expected because the backend HTML is updated
     * *asynchronously* via events. Calling this function by client code often sees a null
     * because backend HTML is not yet create fully initialized as there are pending events.
     */
    public RFunction<Integer,Integer>       minimumHeight;

    /**
     * The position of the component. Usually this is set by a {@link LayoutManager} only.
     */
    public ReadWrite<UIComponent,Position>  position = Property.rw( this, PROP_POSITION );

    /**
     * Implemented via CSS class "Bordered".
     */
    public ReadWrite<UIComponent,Boolean>   bordered = Property.rw( this, "bordered" );

    /**  */
    public ReadWrite<UIComponent,Float>     opacity = Property.rw( this, "opacity" );

    /**  */
    public ReadWrite<UIComponent,Boolean>   enabled = Property.rw( this, "enabled" );

    /** */
    public ReadWrite<UIComponent,LayoutConstraints> layoutConstraints = Property.rw( this, "lc" );

    /**
     * Pseudo property that allows to scroll this component.
     */
    public ReadWrite<?,Align.Vertical>      scrollIntoView = Property.rw( this, "scrollIntoView" );

    /**
     * Pseudo property that allows to set the focus on this component.
     */
    public ReadWrite<?,Boolean>             focus = Property.rw( this, "focus" );

    /** */
    public Events                           events = new Events( this );

    public DecoratorsProperty               decorators = new DecoratorsProperty(this);



    // methods ********************************************

    /**
     * Init
     */
    {
        clientSize = new ReadOnly<UIComponent,Size>( this, "clientSize" ) {
            @Override
            public Opt<Size> opt() {
                return size.opt().ifPresentMap( s -> s.substract( 0, 0 ) );  // FIXME border + scrollbar
            }
        };

//        .defaultsTo( () -> {
//            clientSize.valuePresent = false;
//            var currentSize = size.value();
//            return Size.of( currentSize.width()-10, currentSize.height()-10 );
//        });
    }


    protected UIComponent() {
        UIComponentEvent.manager().publish( new ComponentConstructedEvent( this ) );
    }


    /**
     * Called when this component is added to the component hierarchy.
     */
    protected void attachedTo( UIComposite newParent ) {
        Assert.isNull( parent, "parent should not be set before init()" );
        this.parent = Assert.notNull( newParent  );
        UIComponentEvent.manager().publish( new ComponentAttachedEvent( this, newParent ) );
    }


    /**
     * Called when this component is added to the component hierarchy.
     */
    protected void detachedFrom( @SuppressWarnings("hiding") UIComposite parent ) {
        Assert.isSame( this.parent, parent, "wrong parent" );
        this.parent = null;
        UIComponentEvent.manager().publish( new ComponentDetachedEvent( this ) );
    }


    /**
     * Adds a {@link UIComponentDecorator} to the component.
     * <p>
     * Shortcut to: <code>{@link #decorators}.add(...)</code>
     */
    @SuppressWarnings( "unchecked" )
    public <R extends UIComponentDecorator> Opt<R> addDecorator( R decorator ) {
        return (Opt<R>)decorators.add( decorator );
    }


    /**
     * Destruct this component. If this component is attached to a parent it is
     * automatically removed.
     */
    public void dispose() {
        if (isDisposed()) {
            LOG.info( "DISPOSE: already disposed! (%s)", getClass().getName() );
        }
        else {
            events.dispose();

            decorators.disposeAll();

            data = null;
            if (parent != null) {
                parent.components.remove( this );
            }
            super.dispose();
        }
    }


    public UIComposite parent() {
        return parent;
    }


    @SuppressWarnings("unchecked")
    public <R extends LayoutConstraints> Opt<R> layoutConstraints() {
        return (Opt<R>)layoutConstraints.opt();
    }


    public int computeMinHeight( int width ) {
        return DEFAULT_HEIGHT;
    }

    public int computeMinWidth( int height ) {
        return DEFAULT_HEIGHT;
    }

    @SuppressWarnings( "unchecked" )
    public <R> R setData( String name, R value ) {
        data = data != null ? data : new TreeMap<>();
        return (R)data.put( name, value );
    }

    /**
     * Retrieve/associate additional data.
     */
    @SuppressWarnings("unchecked")
    public <R> R data( String name, Supplier<R> initializer ) {
        data = data != null ? data : new TreeMap<>();
        return (R)data.computeIfAbsent( name, key -> {
            return initializer.get();
        });
    }

    public <R> Opt<R> optData( String name ) {
        return Opt.of( data( name, () -> null ) );
    }

}
