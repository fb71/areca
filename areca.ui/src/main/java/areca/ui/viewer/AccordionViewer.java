/*
 * Copyright (C) 2025, the @authors. All rights reserved.
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
import java.util.NoSuchElementException;

import areca.common.Assert;
import areca.common.base.Opt;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.Property;
import areca.ui.component2.Property.ReadWrite;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComponent.CssStyle;
import areca.ui.component2.UIComposite;
import areca.ui.layout.RowLayout;
import areca.ui.viewer.model.ListModelBase;
import areca.ui.viewer.model.ModelBase;

/**
 *
 * @param <V> The type of the values/items provided by the model.
 * @author Falko Bräutigam
 */
public class AccordionViewer<V>
        extends Viewer<ListModelBase<V>> {

    private static final Log LOG = LogFactory.getLog( AccordionViewer.class );

    private static final CssStyle SHADED = CssStyle.of( "opacity", "0.0" );

    /**
     *
     * @param <VV>
     */
    @FunctionalInterface
    public static interface ModelBuilder<VV> {
        public ListModelBase<? extends VV> buildModel( VV selected, AccordionViewer<VV> viewer );
    }

    // instance *******************************************

    public ReadWrite<AccordionViewer<V>,
            CellBuilder<V>>                     cellBuilder = Property.rw( this, "cellBuilder" );

    public ReadWrite<AccordionViewer<V>,
            ModelBuilder<V>>                    modelBuilder = Property.rw( this, "modelBuilder" );

    /** Spacing between cells. Default: 0 */
    public ReadWrite<AccordionViewer<V>,Integer> spacing = Property.rw( this, "spacing", 0 );

    /** Render lines between rows. Default: false */
    public ReadWrite<AccordionViewer<V>,Boolean> lines = Property.rw( this, "lines", false );

    /** Render odd/even Css classes. Default: false */
    public ReadWrite<AccordionViewer<V>,Boolean> oddEven = Property.rw( this, "oddEven", false );

    /** Show just the selected elements. Default: false */
    public ReadWrite<AccordionViewer<V>,Boolean> exclusive = Property.rw( this, "exclusive", false );

    protected Level                             root, top;


    @Override
    protected boolean isDisposed() {
        return Assert.notNull( root, "No field has been created yet for this viewer." ).container.isDisposed();
    }


    @Override
    public UIComponent create() {
        Assert.isNull( root );

        var container = new UIComposite() {{
            layout.set( RowLayout.verticals().fillWidth( true ).spacing( spacing.$() ) );
            if (configurator != null) {
                configurator.accept( this );
            }
        }};
        model.subscribe( ev -> load() ).unsubscribeIf( () -> container.isDisposed() );

        root = top = new Level( null, null, null, container );
        root.expand();
        return root.container;
    }


    /**
     * One level of the hierarchie/tree.
     */
    protected class Level
            implements CellBuilder<V> {

        public V                    value;

        public UIComponent          head, content;

        public UIComposite          container;

        public Level                parent;

        public Map<V,Level>         children = new HashMap<>();

        public ViewerContext<ListModelBase<? extends V>> ctx;

        public boolean              isExpanded;


        public Level( Level parent, V value, UIComponent head, UIComposite container ) {
            this.container = container;
            this.head = head;
            this.parent = Assert.notSame( this, parent );
            this.value = value;
        }

        protected void expand() {
            Assert.isNull( ctx );
            ctx = new ViewerContext<>();
            ctx.viewer( new CompositeListViewer<V>( this )
                    .spacing.set( spacing.get() )
                    .lines.set( lines.get() )
                    .oddEven.set( oddEven.get() ) );
//                    .onSelect.set( selected -> {
//                        AccordionViewer.this.expand( selected, !isExpanded( selected ) );
//                    }));
            ctx.model( modelBuilder.$().buildModel( value, AccordionViewer.this ) );
            container.add( ctx.createAndLoad() );
            root.container.layout();
            isExpanded = true;
        }

        protected void collapse() {
            for (var child : children.values()) {
                child.collapse();
            }
            children.clear();
            container.components.removeAll();
            root.container.layout();
            ctx = null;
            isExpanded = false;
        }

        @Override
        public UIComponent buildCell( int _index, V _value, ModelBase _model, Viewer<?> _viewer ) {
            return new UIComposite() {{
                lm( RowLayout.verticals().fillWidth( true ) );
                // head
                var _head = add( cellBuilder.$().buildCell( _index, _value, _model, AccordionViewer.this ) );
                // expandable content area
                var _content = add( new UIComposite() {{
                    lm( RowLayout.defaults().fillWidth( true ).margins( 0, 0 ) );
                }});
                children.put( _value, new Level( Level.this, _value, _head, _content ) );
            }};
        }

        protected Opt<Level> find( V v ) {
            if (value == v) {
                return Opt.of( this );
            }
            else {
                for (var child : children.values()) {
                    var result = child.find( v );
                    if (result.isPresent()) {
                        return result;
                    }
                }
                return Opt.absent();
            }
        }
    }


    @Override
    public Object load() {
        root.ctx.load();
        return null;
    }


    public void expand( V item ) {
        var l = root.find( item ).orElseThrow( () -> new NoSuchElementException( "No Level for value: " + item ) );
        l.expand();

        for (var child : l.parent.children.values()) {
            if (child != l) {
                // exclusive childs
                if (exclusive.$()) {
                    child.parent.container.components.remove( child.container );
                }
                // shaded
                else {
                    child.head.styles.add( SHADED );
                }
            }
        }
    }

    public void expand( V item, boolean expand ) {
        if (expand) {
            expand( item );
        } else {
            collapse( item );
        }
    }

    public void collapse( V item ) {
        var l = root.find( item ).orElseThrow( () -> new NoSuchElementException( "No Level for value: " + item ) );
        l.collapse();

        for (var child : l.parent.children.values() ) {
            child.head.styles.remove( SHADED );
        }
    }

    public boolean isExpanded( V item ) {
        var l = root.find( item ).orElseThrow( () -> new NoSuchElementException( "No Level for value: " + item ) );
        return l.isExpanded;
    }

    @Override
    public Object store() {
        return null;
    }
}
