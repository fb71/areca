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

import areca.common.Assert;
import areca.common.base.BiFunction.RBiFunction;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.layout.RowLayout;
import areca.ui.viewer.model.ListModel;

/**
 *
 * @author Falko Bräutigam
 */
public class CompositeListViewer<V>
        extends Viewer<ListModel<V>> {

    private static final Log LOG = LogFactory.getLog( CompositeListViewer.class );

    protected UIComposite container;

    protected RBiFunction<V,ListModel<V>,UIComponent> componentBuilder;

    protected Map<V,UIComponent> components = new HashMap<>();


    public CompositeListViewer( RBiFunction<V,ListModel<V>,UIComponent> componentBuilder ) {
        this.componentBuilder = componentBuilder;
    }


    @Override
    public UIComponent create() {
        Assert.isNull( container );
        container = new UIComposite() {{
            layout.set( RowLayout.verticals().fillWidth( true ).spacing( 20 ) );
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
        for (var v : model) {
            var component = components.computeIfAbsent( v, k -> {
                return componentBuilder.apply( v, model );
            });
            container.add( component );
            hash ^= component.hashCode();
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
