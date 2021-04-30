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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import areca.common.Assert;
import areca.common.AssertionException;
import areca.common.base.Consumer;
import areca.common.base.Sequence;
import areca.ui.Property;
import areca.ui.Size;
import areca.ui.Property.ReadOnly;
import areca.ui.Property.ReadOnlys;
import areca.ui.Property.ReadWrite;
import areca.ui.html.HtmlElement;
import areca.ui.html.HtmlElement.Type;
import areca.ui.html.HtmlNode;
import areca.ui.layout.LayoutManager;

/**
 *
 * @author falko
 */
public class UIComposite
        extends UIComponent {

    private static final Logger LOG = Logger.getLogger( UIComposite.class.getSimpleName() );

    @SuppressWarnings("hiding")
    public static final UIComposite TYPE = new UIComposite();

    /**
     *
     */
    public class Children
            extends ReadOnlys<UIComposite,UIComponent>
            implements Iterable<UIComponent> {

        protected List<UIComponent> list = new ArrayList<>();

        protected Children() {
            super( UIComposite.this, "components" );
        }

        @Override
        public Sequence<UIComponent,RuntimeException> sequence() {
            return Sequence.of( list );
        }

        /**
         * {@link UIComponent#init(UIComposite) Initializes} the given component and adds
         * it to the children of this composite.
         *
         * @see #add(UIComponent)
         * @param child The new child component.
         * @return Newly added/initialized child component.
         */
        @SafeVarargs
        public final <C extends UIComponent,E extends Exception> C add( C child, Consumer<C,E>... initializers ) throws E {
            var childElm = Assert.notNull( child ).init( UIComposite.this );
            htmlElm.children.add( childElm );
            list.add( child );

            for (var initializer : initializers) {
                initializer.accept( child );
            }
            return child;
        }

        void remove( UIComponent child ) {
            if (!list.remove( child )) {
                throw new AssertionException( "Given component is not child of this composite" );
            }
            // must be done by child
            // htmlElm.children.remove( child.htmlElm );
        }

        public void disposeAll() {
            new ArrayList<>( list ).forEach( child -> child.dispose() );
            Assert.that( list.isEmpty() );
        }

        @Override
        public Iterator<UIComponent> iterator() {
            return list.iterator();
        }

        public int size() {
            return list.size();
        }
    };

    // instance *******************************************

    public ReadWrite<UIComposite,LayoutManager> layout = Property.create( this, "layout" );

    public ReadOnly<UIComposite,Size> clientSize = Property.create( this, "clientSize", () -> htmlElm.clientSize.get() );

    public Children components = new Children();


    @Override
    protected HtmlNode init( UIComposite newParent ) {
        htmlElm = new HtmlElement( Type.DIV );
        super.init( newParent );
        return htmlElm;
    }


    @Override
    public void dispose() {
        components.disposeAll();
        super.dispose();
    }


    /**
     * Shortcut to .{@link #components}.{@link Children#add(UIComponent) add}()
     */
    public <C extends UIComponent,E extends Exception> C add( C component, Consumer<C,E> initializer ) throws E {
        return components.add( component, initializer );
    }


    /**
     * Refreshes the layout of the components of this composite.
     *
     * @return this
     */
    public UIComposite layout() {
        layout.opt().ifPresent( lm -> lm.layout( this ) );

        components.sequence()
                .filter( UIComposite.class::isInstance )
                .forEach( c -> ((UIComposite)c).layout() );
        return this;
    }

}
