/*
 * Copyright (C) 2020-2024, the @authors. All rights reserved.
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
package areca.ui.viewer;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;

import areca.common.Assert;
import areca.common.base.BiFunction.RBiFunction;
import areca.common.base.Consumer.RConsumer;
import areca.common.base.Function;
import areca.common.base.Function.RFunction;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.Events.EventType;
import areca.ui.component2.Property;
import areca.ui.component2.Property.ReadWrite;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.layout.RowLayout;
import areca.ui.viewer.model.LazyListModel;
import areca.ui.viewer.model.ListModel;
import areca.ui.viewer.model.ListModelBase;

/**
 * Renders a simple list consisting of plain {@link UIComposite}s which are filled by
 * a given builder function. The container composite does not scroll. Works on
 * {@link ListModel} and {@link LazyListModel}.
 *
 * @author Falko Bräutigam
 */
public class CompositeListViewer<V>
        extends Viewer<ListModelBase<V>> {

    private static final Log LOG = LogFactory.getLog( CompositeListViewer.class );

    // instance *******************************************

    /** Render odd/even Css classes. Default: false */
    public ReadWrite<CompositeListViewer<V>,Boolean> oddEven = Property.rw( this, "oddEven", false );

    /** Spacing between cells. Default: 0 */
    public ReadWrite<CompositeListViewer<V>,Integer> spacing = Property.rw( this, "spacing", 0 );

    /** Render lines between rows. Default: false */
    public ReadWrite<CompositeListViewer<V>,Boolean> lines = Property.rw( this, "lines", false );

    public ReadWrite<CompositeListViewer<V>,RConsumer<V>> onSelect = Property.rw( this, "onSelect" );

    /**
     * Called after loading the list. The default calls {@link UIComposite#layout()}
     * on the root container of this list. This might not be enough if there are new
     * entries which need more space.
     */
    public ReadWrite<CompositeListViewer<V>,RConsumer<UIComposite>> onLayout = Property.rw( this, "onLayout", c -> c.layout() );

    /** A {@link Function} that calculates the current version of an entity in the list. */
    public ReadWrite<CompositeListViewer<V>,RFunction<V,Object>> etag = Property.rw( this, "etag", v -> v.hashCode() );

    protected UIComposite                       container;

    protected CellBuilder<V>                    cellBuilder;

    /** value -> (ETag,UIComposite) */
    protected Map<V,Pair<Object,UIComponent>>   components = new HashMap<>();


    public CompositeListViewer( CellBuilder<V> componentBuilder ) {
        this.cellBuilder = componentBuilder;
    }

    public CompositeListViewer( RFunction<V,UIComponent> f ) {
        this.cellBuilder = (_index, _value, _model, _viewer) -> f.apply( _value );
    }

    @SuppressWarnings( "unchecked" )
    public CompositeListViewer( RBiFunction<V,ListModelBase<V>,UIComponent> f ) {
        this.cellBuilder = (_index, _value, _model, _viewer) -> f.apply( _value, (ListModelBase<V>)_model );
    }


    @Override
    public UIComponent create() {
        Assert.isNull( container );
        container = new UIComposite() {{
            layout.set( RowLayout.verticals().fillWidth( true ).spacing( spacing.$() ) );
            if (configurator != null) {
                configurator.accept( this );
            }
        }};
        model.subscribe( ev -> load() ).unsubscribeIf( () -> container.isDisposed() );
        return container;
    }


    @Override
    protected boolean isDisposed() {
        return Assert.notNull( container, "No field has been created yet for this viewer." ).isDisposed();
    }


    @Override
    public Object load() {
        var index = new MutableInt( 0 );
        // LazyListModel
        if (model instanceof LazyListModel) {
            var lazy = (LazyListModel<V>)model;
            lazy.load( 0, 100 ).onSuccess( opt -> { // 100!?
                if (index.intValue() == 0) {
                    // after wait to avoid flicker
                    container.components.removeAll();
                }
                if (opt.isPresent()) {
                    container.add( buildItem( opt.get(), index ) );
                    index.increment();
                }
                else {
                    onLayout.get().accept( container );
                };
            });
            return null; // XXX
        }
        // ListModel
        else if (model instanceof ListModel) {
            container.components.removeAll();
            var hash = 0;
            for (var v : (ListModel<V>)model) {
                container.add( buildItem( v, index ) );
                hash ^= buildItem( v, index ).hashCode();
                index.increment();
            }
            onLayout.get().accept( container );
            return hash; // XXX
        }
        else {
            throw new RuntimeException( "Unknown model type: " + model );
        }
    }


    protected UIComponent buildItem( V v, MutableInt index ) {
        var entry = components.compute( v, (k,current) -> {
            var newETag = etag.get().apply( v );
            if (current != null && Objects.equals( current.getLeft(), newETag )) {
                return current;
            }
            else {
                var result = cellBuilder.buildCell( index.intValue(), v, model, this );
                result.cssClasses.add( "TableCell" );
                if (lines.$()) {
                    result.cssClasses.add( "Lines" );
                }
                if (onSelect.opt().isPresent()) {
                    result.cssClasses.add( "Clickable" );
                    result.events.on( EventType.SELECT, ev -> onSelect.$().accept( v ) );
                }
                return Pair.of( newETag, result );
            }
        });
        var component = entry.getRight();

        // index can change after add/remove
        if (oddEven.$()) {
            component.cssClasses.remove( "Odd" );
            component.cssClasses.remove( "Even" );
            component.cssClasses.add( index.intValue() % 2 == 1 ? "Odd" : "Even" );
        }
        return component;
    }


    @Override
    public Object store() {
        return null;
//        var value = textField.content.opt().orNull();
//        model.set( value );
//        return value;
    }

}
