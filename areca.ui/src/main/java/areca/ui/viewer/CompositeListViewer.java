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
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.Events.EventType;
import areca.ui.component2.Events.UIEvent;
import areca.ui.component2.Property;
import areca.ui.component2.Property.ReadWrite;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.layout.RowLayout;
import areca.ui.viewer.model.ListModel;

/**
 * Renders a simple list consisting of plain {@link UIComposite} which are filled by
 * a given builder function. The container composite does not scroll.
 *
 * @author Falko Bräutigam
 */
public class CompositeListViewer<V>
        extends Viewer<ListModel<V>> {

    private static final Log LOG = LogFactory.getLog( CompositeListViewer.class );

    /** Render odd/even Css classes. Default: false */
    public ReadWrite<CompositeListViewer<V>,Boolean> oddEven = Property.rw( this, "oddEven", false );

    /** Spacing between cells. Default: 0 */
    public ReadWrite<CompositeListViewer<V>,Integer> spacing = Property.rw( this, "spacing", 0 );

    /** Render lines between rows. Default: false */
    public ReadWrite<CompositeListViewer<V>,Boolean> lines = Property.rw( this, "lines", false );

    public ReadWrite<CompositeListViewer<V>,RConsumer<UIEvent>> onSelect = Property.rw( this, "onSelect" );

    protected UIComposite           container;

    protected CellBuilder<V>        componentBuilder;

    protected Map<V,UIComponent>    components = new HashMap<>();

    public interface CellBuilder<T>
            extends RBiFunction<T,ListModel<T>,UIComponent> {
    }

    public CompositeListViewer( CellBuilder<V> componentBuilder ) {
        this.componentBuilder = componentBuilder;
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
        container.components.removeAll();
        var hash = 0;
        var index = new MutableInt( 0 );
        for (var v : model) {
            var component = components.computeIfAbsent( v, k -> {
                var result = componentBuilder.apply( v, model );
                result.cssClasses.add( "TableCell" );
                if (lines.$()) {
                    result.cssClasses.add( "Lines" );
                }
                if (onSelect.opt().isPresent()) {
                    result.cssClasses.add( "Clickable" );
                    result.events.on( EventType.SELECT, onSelect.$() );
                }
                return result;
            });
            if (oddEven.$()) {
                component.cssClasses.remove( "Odd" );
                component.cssClasses.remove( "Even" );
                component.cssClasses.add( index.intValue() % 2 == 1 ? "Odd" : "Even" );
            }
            container.add( component );
            hash ^= component.hashCode();
            index.increment();
        }
        container.layout();
        return hash; // XXX
    }


    @Override
    public Object store() {
        return null;
//        var value = textField.content.opt().orNull();
//        model.set( value );
//        return value;
    }

}
