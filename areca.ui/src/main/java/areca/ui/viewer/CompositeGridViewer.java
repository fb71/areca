/*
 * Copyright (C) 2020, the @authors. All rights reserved.
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

import org.apache.commons.lang3.mutable.MutableInt;
import areca.common.Assert;
import areca.common.base.BiFunction.RBiFunction;
import areca.common.base.Consumer.RConsumer;
import areca.common.base.Function.RFunction;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.Events.EventType;
import areca.ui.component2.Property;
import areca.ui.component2.Property.ReadWrite;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.layout.RasterLayout;
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
public class CompositeGridViewer<V>
        extends Viewer<ListModelBase<V>> {

    private static final Log LOG = LogFactory.getLog( CompositeGridViewer.class );

    /**
     *
     * @param <T>
     * @author Falko Bräutigam
     */
    public interface CellBuilder<T>
            extends RBiFunction<T,ListModelBase<T>,UIComponent> {
    }

    // instance *******************************************

    /** Spacing between cells. Default: 0 */
    public ReadWrite<CompositeGridViewer<V>,Integer> spacing = Property.rw( this, "spacing", 0 );

    /**  */
    public ReadWrite<CompositeGridViewer<V>,Integer> columns = Property.rw( this, "columns", 3 );

    public ReadWrite<CompositeGridViewer<V>,RConsumer<V>> onSelect = Property.rw( this, "onSelect" );

    protected UIComposite           container;

    protected CellBuilder<V>        componentBuilder;

    protected Map<V,UIComponent>    components = new HashMap<>();


    public CompositeGridViewer( CellBuilder<V> componentBuilder ) {
        this.componentBuilder = componentBuilder;
    }

    public CompositeGridViewer( RFunction<V,UIComponent> componentBuilder ) {
        this.componentBuilder = (value,__) -> componentBuilder.apply( value );
    }


    @Override
    public UIComponent create() {
        Assert.isNull( container );
        container = new UIComposite() {{
            layout.set( RasterLayout.colums( columns.$() ).spacing( spacing.$() ) );
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
        var c = new MutableInt( 0 );

        // LazyListModel
        if (model instanceof LazyListModel) {
            ((LazyListModel<V>)model).load( 0, 100 ).onSuccess( opt -> { // 100!?
                if (c.intValue() == 0) {
                    container.components.removeAll(); // after wait to avoid flicker
                }
                if (opt.isPresent()) {
                    container.add( buildItem( opt.get(), c ) );
                    c.increment();
                }
                else {
                    container.layout();
                };
            });
            return null; // XXX
        }
//        // ListModel
//        else if (model instanceof ListModel) {
//            container.components.removeAll();
//            var hash = 0;
//            for (var v : (ListModel<V>)model) {
//                container.add( buildItem( v, index ) );
//                hash ^= buildItem( v, index ).hashCode();
//                index.increment();
//            }
//            container.layout();
//            return hash; // XXX
//        }
        else {
            throw new RuntimeException( "Unknown model type: " + model );
        }
    }


    protected UIComponent buildItem( V v, MutableInt index ) {
        var component = components.computeIfAbsent( v, k -> {
            var result = componentBuilder.apply( v, model );
            result.cssClasses.add( "TableCell" );
            if (onSelect.opt().isPresent()) {
                result.cssClasses.add( "Clickable" );
                result.events.on( EventType.SELECT, ev -> onSelect.$().accept( v ) );
            }
            return result;
        });
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
