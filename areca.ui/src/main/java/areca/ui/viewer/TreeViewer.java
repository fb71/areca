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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;

import areca.common.Assert;
import areca.common.base.Opt;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.Property;
import areca.ui.component2.Property.ReadWrite;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComponent.CssStyle;
import areca.ui.component2.UIComposite;
import areca.ui.viewer.model.LazyTreeModel;

/**
 *
 * @param <V> The type of the values/items provided by the model.
 * @author Falko Bräutigam
 * @see DrillingTreeLayout
 * @see StandardTreeLayout
 */
public class TreeViewer<V>
        extends Viewer<LazyTreeModel<V>> {

    private static final Log LOG = LogFactory.getLog( TreeViewer.class );

    private static final CssStyle SHADED = CssStyle.of( "opacity", "0.0" );

    /**
     * The layout is responsible for creating the actual UI components.
     */
    public static abstract class TreeViewerLayout<VV> {

        public abstract void expand( TreeViewer<VV>.Level l );

        public abstract void collapse( TreeViewer<VV>.Level l );

        public abstract void update( TreeViewer<VV>.Level l );

        public abstract UIComposite init( TreeViewer<VV> viewer );

        public void dispose() {
        }
    }


    // instance *******************************************

    /** The {@link CellBuilder} to be used to render the cels of the tree. */
    public ReadWrite<TreeViewer<V>,
            CellBuilder<V>>                 cellBuilder = Property.rw( this, "cellBuilder" );

    /**
     * The actual {@link TreeViewerLayout layout} to be used to render this tree.
     * Default: {@link StandardTreeLayout}
     */
    public ReadWrite<TreeViewer<V>,
            TreeViewerLayout<V>>            treeLayout = Property.rw( this, "layout", new StandardTreeLayout<>() );

    /** Spacing between cells. Default: 0 */
    public ReadWrite<TreeViewer<V>,Integer> spacing = Property.rw( this, "spacing", 0 );

    /** Render lines between rows. Default: false */
    public ReadWrite<TreeViewer<V>,Boolean> lines = Property.rw( this, "lines", false );

    /** Render odd/even Css classes. Default: false */
    public ReadWrite<TreeViewer<V>,Boolean> oddEven = Property.rw( this, "oddEven", false );

    /** Show just the selected elements. Default: false */
    public ReadWrite<TreeViewer<V>,Boolean> exclusive = Property.rw( this, "exclusive", false );

    protected Level                         root, top;

    protected UIComposite                   container;


    @Override
    protected boolean isDisposed() {
        return Assert.notNull( container, "No field has been created yet for this viewer." ).isDisposed();
    }


    @Override
    public UIComponent create() {
        Assert.isNull( root );
        root = top = new Level( null, null );

        container = treeLayout.$().init( this );

        if (configurator != null) {
            configurator.accept( container );
        }

        model.subscribe( ev -> update( root ) ).unsubscribeIf( () -> container.isDisposed() );

        //load();
        return container;
    }


    @Override
    public Object load() {
        expand( null );
        return null;
    }


    /**
     * Recursively find structural changes and update the UI, starting from the given
     * level.
     */
    protected void update( Level l ) {
        if (l.isExpanded) {
            model.loadAllChildren( l.value ).onSuccess( update -> {
                var current = Sequence.of( l.children ).map( c -> c.value ).toSet();
                if (!current.equals( new HashSet<>( update ) )) { // ignore order
                    l.children = Sequence.of( update ).map( c -> new Level( l, c ) ).toList();
                    treeLayout.$().update( l );
                }
                else {
                    l.children.forEach( child -> update( child ) );
                }
            });
        }
    }


    public void expand( V item ) {
        var l = root.find( item ).orElseThrow( () -> new NoSuchElementException( "No Level for value: " + item ) );
        if (!l.isExpanded) {
            l.isExpanded = true;
            top = l;

            model.loadAllChildren( item ).onSuccess( children -> {
                Assert.that( l.children.isEmpty() );
                for (var child : children) {
                    l.children.add( new Level( l, child ) );
                }
                treeLayout.$().expand( l );
            });
        }
    }


    public void collapse( V item ) {
        var level = root.find( item ).orElseThrow( () -> new NoSuchElementException( "No Level for value: " + item ) );

        for (var l = top; l != level.parent; l = l.parent) {
            treeLayout.$().collapse( l );
            l.isExpanded = false;
            l.children.clear();
        }
    }


    public void expand( V item, boolean expand ) {
        if (expand) {
            expand( item );
        } else {
            collapse( item );
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


    /**
     * One level of the hierarchie/tree.
     */
    protected class Level {

        public V                    value;

        //public UIComponent          head, content;

        public Level                parent;

        public List<Level>          children = new ArrayList<>();

        public boolean              isExpanded;


        public Level( Level parent, V value ) {
            this.parent = Assert.notSame( this, parent );
            this.value = value;
        }

        protected Opt<Level> find( V v ) {
            if (value == v) {
                return Opt.of( this );
            }
            else {
                for (var child : children) {
                    var result = child.find( v );
                    if (result.isPresent()) {
                        return result;
                    }
                }
                return Opt.absent();
            }
        }
    }
}
