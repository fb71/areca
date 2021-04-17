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
import areca.common.base.Consumer;
import areca.common.base.Sequence;
import areca.ui.Size;
import areca.ui.component.Property.ReadOnly;
import areca.ui.component.Property.ReadWrite;
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

    public ReadWrite<LayoutManager> layout = Property.create( this, "lm" );

    public ReadOnly<Size>           clientSize = Property.create( this, "clientSize", () -> htmlElm.clientSize.get() );

    private List<UIComponent>       components = new ArrayList<>();


    @Override
    protected HtmlNode init( UIComposite newParent ) {
        htmlElm = new HtmlElement( Type.DIV );
        super.init( newParent );
        return htmlElm;
    }


    /**
     * {@link UIComponent#init(UIComposite) Initializes} the given component and adds
     * it to the children of this composite.
     *
     * @see #add(UIComponent)
     * @param component The new child component.
     * @return Newly added/initialized child component.
     */
    public <C extends UIComponent> C add( C component ) {
        var childElm = Assert.notNull( component ).init( this );
        htmlElm.children.add( childElm );
        components.add( component );
        return component;
    }


    /**
     * {@link UIComponent#init(UIComposite) Initializes} the given component and adds
     * it to the children of this composite.
     *
     * @see #add(UIComponent)
     * @param component The new child component.
     * @return Newly added/initialized child component.
     */
    public <C extends UIComponent,E extends Exception> C add( C component, Consumer<C,E> initializer ) throws E {
        add( component );
        Assert.notNull( initializer ).accept( component );
        return component;
    }


    /**
     * Refreshes the layout of the components of this composite.
     *
     * @return this
     */
    public UIComposite layout() {
        layout.get().layout( this );

        // components.stream().filter( UIComposite.class::isInstance ).forEach( c -> ((UIComposite)c).layout() );
        for (UIComponent component : components) {
            if (component instanceof UIComposite) {
                ((UIComposite)component).layout();
            }
        }
        return this;
    }


    public ExtIterable<UIComponent> components() {
        return new ExtIterable<UIComponent>() {
            @Override
            public Iterator<UIComponent> iterator() {
                return components.iterator();
            }
            @Override
            public int size() {
                return components.size();
            }
        };
    }


    /**
     *
     */
    public interface ExtIterable<T>
            extends Iterable<T> {

        public int size();

//        public default Stream<T> stream() {
//            return StreamSupport.stream( spliterator(), false );
//        }

        public default Sequence<T,RuntimeException> sequence() {
            return Sequence.of( this );
        }
    }

}
