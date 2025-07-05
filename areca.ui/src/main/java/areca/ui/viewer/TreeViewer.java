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
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;

import areca.common.Assert;
import areca.common.Promise;
import areca.common.base.Opt;
import areca.common.base.Predicate.RPredicate;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.Property;
import areca.ui.component2.Property.ReadWrite;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComponent.CssStyle;
import areca.ui.component2.UIComposite;
import areca.ui.layout.LayoutConstraints;
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
     * The layout is responsible for creating/managing the actual UI components.
     */
    public static abstract class TreeViewerLayout<VV> {

        public abstract void update( TreeViewer<VV>.Changes changed );

        public abstract UIComposite init( TreeViewer<VV> viewer );

        public abstract void dispose();
    }

    /**
     * Signals the {@link TreeViewer} that a cell returned by its {@link CellBuilder}
     * wants to get informed about expand/collapse state changes.
     */
    public interface ExpandableCell {

        /**
         * Called by the {@link TreeViewer} ({@link TreeViewerLayout}) when the
         * expand state of the value connected to this cell has changed (by user
         * click or programmatically).
         */
        public void updateExpand( boolean expanded );
    }


    // instance *******************************************

    /**
     * The {@link CellBuilder} to be used to render the cells of the tree. The
     * {@link LayoutConstraints} of the cells depends on the actual
     * {@link #treeLayout} ({@link TreeViewerLayout}) in use.
     *
     * @see ExpandableCell
     */
    public ReadWrite<TreeViewer<V>,
            CellBuilder<V>>                 cellBuilder = Property.rw( this, "cellBuilder" );

    /**
     * The {@link TreeViewerLayout layout} to be used to render this tree.
     * Default: {@link StandardTreeLayout}
     *
     * @see DrillingTreeLayout2
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


    @Override
    public Object store() {
        return null;
    }


    /**
     * Recursively find structural changes and update the UI, starting from the given
     * level.
     */
    protected Promise<Changes> update( Level l ) {
        var changes = new Changes();
        return doUpdate( l ).map( __ -> {
            treeLayout.$().update( changes.fill() );
            LOG.debug( "update(): %s", changes );
            return changes;
        });
    }


    protected Promise<?> doUpdate( Level l ) {
        if (l.isExpanded) {
            return model.loadAllChildren( l.value ).then( update -> {
                var current = Sequence.of( l.children ).map( c -> c.value ).toSet();
                if (!current.equals( new HashSet<>( update ) )) { // ignore order
                    LOG.debug( "Updated children: %s", l.value );
                    l.children = Sequence.of( update ).map( c -> new Level( l, c ) ).toList();
                    return Promise.async( null );
                }
                else {
                    //l.children.forEach( child -> doUpdate( child ) );
                    return Promise
                            .serial( l.children, null, child -> doUpdate( child ) )
                            .reduce2( null, (r,child) -> null );
                }
            });
        }
        else {
            return Promise.async( null );
        }
    }


    /**
     * {@link #expand(Object)} or {@link #collapse(Object)} the given item depending
     * on the given parameter.
     */
    public Promise<Changes> expand( V item, boolean expand ) {
        if (expand) {
            return expand( item );
        } else {
            return collapse( item );
        }
    }

    /**
     * {@link #expand(Object)} or {@link #collapse(Object)} the given item depending
     * on the curent state.
     *
     * @return True if the item is now expanded.
     */
    public Promise<Boolean> toggle( V item ) {
        var l = root.find( item ).orElseError( "No Level for value: %s", item );
        return expand( item, !l.isExpanded ).map( __ -> l.isExpanded );
    }


    /**
     * Expands the given top-level item. The item must be loaded/visible currently.
     * Does nothing if the item is already expanded.
     *
     * @return A {@link Promise} providing the children of the expanded item.
     */
    public Promise<Changes> expand( V item ) {
        var changes = new Changes();
        return doExpand( item ).map( __ -> {
            treeLayout.$().update( changes.fill() );
            LOG.debug( "expand(): %s", changes );
            return changes;
        });
    }


    /**
     * Collapses the given item and all possibly expanded children. Does nothing if
     * the item is currently collapsed.
     */
    public Promise<Changes> collapse( V item ) {
        var changes = new Changes();
        doCollapse( item );
        treeLayout.$().update( changes.fill() );
        LOG.debug( "collapse(): %s", changes );
        return Promise.async( changes );
    }


    /**
     *
     */
    @SuppressWarnings( "unchecked" )
    public Promise<List<? extends V>> expandPath( V... path ) {
        var changes = new Changes();
        return doExpandPath( path ).onSuccess( __ -> {
            treeLayout.$().update( changes.fill() );
            LOG.debug( "expandPath(): %s", changes );
        });
    }


    @SuppressWarnings( "unchecked" )
    protected Promise<List<? extends V>> doExpandPath( V... path ) {
        var item = path[0];
        var nextPath = ArrayUtils.remove( path, 0 );

        return doExpand( item ).then( __ -> {
            return nextPath.length > 0
                    ? doExpandPath( nextPath )
                    : Promise.async( __ );
        });
    }


    protected Promise<List<? extends V>> doExpand( V item ) {
        var l = root.find( item ).orElseError( "No Level for value: %s", item );

        if (l.isExpanded) {
            return Promise.async( l.children().map( child -> child.value ).toList() );
        }

        // close siblings if "exclusive"
        if (exclusive.$() && l.parent != null) {
            l.parent.expandedChildren().forEach( child -> doCollapse( child.value ) );
        }

        return model.loadAllChildren( item ).onSuccess( children -> {
            Assert.that( l.children.isEmpty() );
            l.isExpanded = true;
            for (var child : children) {
                l.children.add( new Level( l, child ) );
            }
        });
    }


    protected void doCollapse( V item ) {
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
            expanded.isExpanded = false;
            expanded.children.clear();
        }
    }


    public boolean isExpanded( V item ) {
        return root.find( item ).orElseError( "No Level for value: %s", item ).isExpanded;
    }


    /**
     * The result of an update operation.
     */
    public class Changes {
        /** The levels that have changed expand state. */
        public Set<Level> toggled;
        public Set<Level> created;
        public Set<Level> removed;
        public Set<Level> modified;
        private Set<Level> previous;

        private Changes() {
            previous = root.allMatching( new HashSet<>(), l -> true );
            toggled = root.allMatching( new HashSet<>(), l -> l.isExpanded );
        }

        public Changes fill() {
            toggled = root.allMatching( new HashSet<>(), l -> l.isExpanded ^ toggled.contains( l ) );

            created = root.allMatching( new HashSet<>(), l -> !previous.contains( l ) );

            var all = root.allMatching( new HashSet<>(), l -> true );
            removed = new HashSet<>( previous );
            removed.removeIf( l -> all.contains( l ) );
            return this;
        }

        @Override
        @SuppressWarnings( "removal" )
        public String toString() {
            return "TreeViewer.Changes [toggled=%s, created=%s, removed=%s, modified=?]".formatted(
                    toggled.size(), created.size(), removed.size() );
        }
    }


    /**
     * One level (node) of the hierarchie/tree.
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

        /** Support {@link Changes} to find changes. */
        @SuppressWarnings( "unchecked" )
        public boolean equals( Object obj ) {
            if (obj instanceof TreeViewer.Level) {
                var other = (TreeViewer<V>.Level)obj;
                return Objects.equals( parent, other.parent ) && Objects.equals( value, other.value );
            }
            return false;
        }

        /** Support {@link Changes} to find changes. */
        public int hashCode() {
            return value != null ? value.hashCode() : 31;
        }

        public Opt<Level> find( V v ) {
            var result = allMatching( new ArrayList<>(), l -> Objects.equals( l.value, v ) );
            Assert.that( result.size() <= 1, "find(): v = " + v + ", results = " + result );
            return result.isEmpty() ? Opt.absent() : Opt.of( result.get( 0 ) );
        }

        public List<Level> allMatching( RPredicate<Level> p ) {
            return allMatching( new ArrayList<>(), p );
        }

        public <R extends Collection<Level>> R allMatching( R result, RPredicate<Level> p ) {
            if (p.test( this )) {
                result.add( this );
            }
            for (var child : children) {
                child.allMatching( result, p );
            }
            return result;
        }

        public Sequence<TreeViewer<V>.Level,RuntimeException> children() {
            return Sequence.of( children );
        }

        public Sequence<TreeViewer<V>.Level,RuntimeException> expandedChildren() {
            return children().filter( c -> c.isExpanded );
        }
    }
}
