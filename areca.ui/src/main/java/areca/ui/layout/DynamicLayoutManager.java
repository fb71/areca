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
package areca.ui.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import areca.common.Assert;
import areca.common.Promise;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.Property;
import areca.ui.component2.Property.ReadWrite;
import areca.ui.component2.ScrollableComposite;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;

/**
 * Requests components to be created by the composite on-demand.
 * <p/>
 * For data backed layout, like table or tree.
 *
 * @author Falko Bräutigam
 */
public abstract class DynamicLayoutManager<C extends DynamicLayoutManager.Component>
        extends LayoutManager {

    private static final Log LOG = LogFactory.getLog( DynamicLayoutManager.class );

    public ReadWrite<DynamicLayoutManager<C>,ComponentProvider<C>> provider = Property.rw( this, "provider" );

    protected ScrollableComposite               scrollable;

    private Map<Integer,C>                      cache = new TreeMap<>();


    /**
     * (Re-)layout the entiry content of this {@link DynamicLayoutManager}. Flushing
     * layout caches.
     */
    @Override
    public void layout( UIComposite composite ) {
        Assert.that( scrollable == null || scrollable == composite );
        Assert.that( composite instanceof ScrollableComposite, "Composite is not a ScrollableComposite!" );
        scrollable = (ScrollableComposite)composite;
        cache.clear();
    }


    public abstract void componentHasChanged( Component changed );


    /**
     * Provides the specified components by either delivering from the cache or
     * requesting new components from composite.
     */
    protected Promise<List<C>> requestComponents( int start, int num ) {
        var result = new ArrayList<C>( num );

        // check what is already in the cache
        var providerStart = start;
        var providerNum = num;
        for (int i = 0; i < num; i++) {
            var c = cache.get( i + start );
            if (c != null) {
                providerStart ++;
                providerNum --;
                result.add( c );
            }
        }
        // ask provider for the rest
        return provider.$()
                .provide( providerStart, providerNum )
                .map( provided -> {
                    for (var c : provided) {
                        if (cache.putIfAbsent( c.index, c ) == null) {
                            scrollable.add( c.component );
                            result.add( c );
                        }
                        else {
                            c.component.dispose();
                        }
                    }
                    return result;
                });
    }


    /**
     * Provides the specified components *just* if they are not already in the cache.
     *
     * @return Newly created List.
     */
    protected Promise<List<C>> ensureComponents( int start, int num ) {
        // check what is already in the cache
        var providerStart = start;
        var providerNum = num;
        for (int i = 0; i < num; i++) {
            var c = cache.get( i + start );
            if (c != null) {
                providerStart ++;
                providerNum --;
            }
        }
        // ask provider for the rest
        LOG.debug( "ensureComponents(): (start=%d, num=%s) -> (%d/%d)", start, num, providerStart, providerNum );
        return provider.$()
                .provide( providerStart, providerNum )
                .map( provided -> {
                    LOG.debug( "ensureComponents(): provided=%d", provided.size() );
                    var result = new ArrayList<C>( provided.size() );
                    for (var c : provided) {
                        if (cache.putIfAbsent( c.index, c ) == null) {
                            scrollable.add( c.component );
                            result.add( c );
                        }
                        else {
                            c.component.dispose();
                        }
                    }
                    return result;
                });
    }


    /**
     *
     */
    @FunctionalInterface
    public interface ComponentProvider<C extends Component> {

        /**
         * Create new components for the requested index range. The implementation
         * should cache the results if the layout is frequently updated (re-layouted)
         * to reflect changes in the underlying model.
         */
        public Promise<List<C>> provide( int start, int num );
    }


    /**
     * Wraps a {@link UIComponent} and its index in a {@link DynamicLayoutManager}.
     */
    public static class Component {

        public UIComponent  component;

        public int          index;

        public Component( UIComponent component, int index ) {
            this.component = component;
            this.index = index;
        }
    }
}
