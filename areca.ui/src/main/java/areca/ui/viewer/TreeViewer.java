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

import static java.util.Arrays.asList;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import areca.common.Assert;
import areca.common.Platform;
import areca.common.Promise;
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

    /** Collapse an opened branch when a new one is expanded. Default: false */
    public ReadWrite<TreeViewer<V>,Boolean> exclusive = Property.rw( this, "exclusive", false );

    protected Level                         root;

    protected UIComposite                   container;


    @Override
    protected boolean isDisposed() {
        return Assert.notNull( container, "No field has been created yet for this viewer." ).isDisposed();
    }


    @Override
    public UIComponent create() {
        Assert.isNull( root );
        root = new Level( null, null );

        container = treeLayout.$().init( this );
        if (configurator != null) {
            configurator.accept( container );
        }

        model.subscribe( ev -> update( root ) ).unsubscribeIf( () -> container.isDisposed() );

        return container;
    }


    @Override
    public Object load() {
        expand( (V)null );
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


    /**
     * See {@link #expandPath(List)}
     */
    public Promise<?> expandPath( @SuppressWarnings( "unchecked" ) V... path ) {
        if (path.length == 0) {
            return Promise.async( null );
        }
        else {
            var item = path[0];
            var nextPath = ArrayUtils.remove( path, 0 );
            return expand( item )
                    .then( __ -> Platform.schedule( 500, () -> __ ) )
                    .then( __ -> expandPath( nextPath ) );
        }
    }


    /**
     * Expands the given top-level item. The item must be loaded/visible currently.
     * Does nothing if the item is already expanded.
     *
     * @return A {@link Promise} providing the children of the expanded item.
     */
    public Promise<List<? extends V>> expand( V item ) {
        var l = root.find( item ).orElseError( "No Level for value: %s", item );
        if (l.isExpanded) {
            return Promise.async( l.children().map( child -> child.value ).toList() );
        }

        // close siblings if "exclusive"
        if (exclusive.$()) {
            for (var child : l.children) {
                if (child.isExpanded) {
                    collapse( child.value );
                }
            }
        }

        return model.loadAllChildren( item ).onSuccess( children -> {
            Assert.that( l.children.isEmpty() );
            l.isExpanded = true;
            for (var child : children) {
                l.children.add( new Level( l, child ) );
            }
            treeLayout.$().expand( l );
        });
    }


    /**
     * Collapses the given item and all possibly expanded children. Does nothing if
     * the item is currently collapsed.
     */
    public void collapse( V item ) {
        var level = root.find( item ).orElseError( "No Level for value: %s", item );
        if (!level.isExpanded) {
            return;
        }

        // find all expanded children
        var expandedChildren = new LinkedList<Level>();
        var deque = new ArrayDeque<Level>( asList( level ) );
        for (var l = deque.poll(); l != null; l = deque.poll()) {
            if (l.isExpanded) {
                expandedChildren.add( 0, l );
            }
            deque.addAll( l.children );
        }
        // close from top to the given item
        for (var expanded : expandedChildren) {
            treeLayout.$().collapse( expanded );
            expanded.isExpanded = false;
            expanded.children.clear();
        }
    }


    /**
     * {@link #expand(Object)} or {@link #collapse(Object)} the given item depending
     * on the given parameter.
     */
    public void expand( V item, boolean expand ) {
        if (expand) {
            expand( item );
        } else {
            collapse( item );
        }
    }


    public boolean isExpanded( V item ) {
        return root.find( item ).orElseError( "No Level for value: %s", item ).isExpanded;
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

        public Level                parent;

        public List<Level>          children = new ArrayList<>();

        public boolean              isExpanded;


        public Level( Level parent, V value ) {
            this.parent = Assert.notSame( this, parent );
            this.value = value;
        }

        public Opt<Level> find( V v ) {
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

        public Sequence<TreeViewer<V>.Level,RuntimeException> children() {
            return Sequence.of( children );
        }
    }
}
