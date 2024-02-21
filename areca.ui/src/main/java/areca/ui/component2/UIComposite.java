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

import java.util.ArrayList;
import areca.common.Assert;
import areca.common.base.Consumer;
import areca.common.base.Opt;
import areca.ui.component2.Property.ReadWrite;
import areca.ui.component2.Property.ReadWrites;
import areca.ui.layout.LayoutManager;

/**
 *
 * @author falko
 */
public class UIComposite
        extends UIComponent {

    public static final String PROP_COMPONENTS = "components";
    public static final String PROP_LAYOUT = "layout";

    public ReadWrite<UIComposite,LayoutManager> layout = Property.rw( this, PROP_LAYOUT );

    public Children             components = new Children();


//    static {
//        var updates = new HashSet<UIComposite>();
//
//        UIComponentEvent.manager()
//                .subscribe( (PropertyChangedEvent<UIComposite> ev) -> {
//                    updates.add( (UIComposite)ev.getSource().component() );
//                    if (updates.size() == 1) {
//                        Platform.schedule( 100, () -> {
//                            updates.forEach( update -> update.layout() );
//                            updates.clear();
//                        });
//                    }
//                })
//                .performIf( ev -> ev instanceof PropertyChangedEvent && (
//                        ((PropertyChangedEvent<?>)ev).getSource().name().equals( "layout" ) ||
//                        ((PropertyChangedEvent<?>)ev).getSource().name().equals( "components" ))
//                );
//    }


    @Override
    public void dispose() {
        components.disposeAll();
        super.dispose();
    }


    /**
     * Adds a {@link UIComponent} child the this composite.
     * <p>
     * Shortcut to <code>{@link #components}.add()</code>
     */
    @SuppressWarnings("unchecked")
    public <C extends UIComponent,E extends Exception> C add( C component ) throws E {
        return (C)components.add( component ).orElseError();
    }


    @SuppressWarnings("unchecked")
    public <C extends UIComponent,E extends Exception> C add( C component, Consumer<C,E> initializer ) throws E {
        return (C)components.add( component )
                .ifPresent( __ -> initializer.accept( component ) )
                .orElseError();
    }


    /**
     * Refreshes the layout of the components of this composite.
     *
     * @return this
     */
    public UIComposite layout() {
        layout.opt().ifPresent( lm -> lm.layout( this ) );

        for (var child : components.value()) {
            if (child instanceof UIComposite ) {
                ((UIComposite)child).layout();
            }
        }
        return this;
    }


//    @Override
//    public int computeMinWidth( int height ) {
//        return layout.opt().map( l -> l.computeMinWidth( height ) ).orElse( 100 );
//    }
//
//
//    @Override
//    public int computeMinHeight( int width ) {
//        return layout.opt().map( l -> l.computeMinHeight( width ) ).orElse( 100 );
//    }


    /**
     *
     */
    public class Children
            extends ReadWrites<UIComposite,UIComponent> {

        protected Children() {
            super( UIComposite.this, PROP_COMPONENTS );
            rawSet( new ArrayList<>() );
        }

        @Override
        public Opt<UIComponent> add( UIComponent add ) {
            return super.add( Assert.notNull( add ) ).ifPresent( __ -> {
                add.attachedTo( UIComposite.this );
            });
        }

        @Override
        public Opt<UIComponent> remove( UIComponent remove ) {
            return super.remove( remove ).ifPresent( __ -> {
                remove.detachedFrom( UIComposite.this );
            });
        }

        public void disposeAll() {
            new ArrayList<>( value ).forEach( child -> child.dispose() );
            Assert.that( value.isEmpty() );
        }

        public int size() {
            return value.size();
        }
    }

}
