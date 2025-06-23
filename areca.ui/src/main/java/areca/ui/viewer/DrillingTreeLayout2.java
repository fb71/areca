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
import java.util.HashMap;
import java.util.Map;
import areca.common.Assert;
import areca.common.Platform;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComponent.CssStyle;
import areca.ui.component2.UIComposite;
import areca.ui.layout.RowLayout;
import areca.ui.viewer.TreeViewer.ExpandableCell;
import areca.ui.viewer.TreeViewer.TreeViewerLayout;

/**
 * A {@link TreeViewer} layout that shows just the one, expanded branch of the tree.
 *
 * @author Falko Bräutigam
 */
public class DrillingTreeLayout2<V>
        extends TreeViewerLayout<V> {

    private static final Log LOG = LogFactory.getLog( DrillingTreeLayout2.class );

    /**
     * Delay between creating a branch and the call of
     * {@link ExpandableCell#updateExpand(boolean)} when expanded.
     */
    public static final int BRANCH_UPDATE_EXPAND_DELAY = 750;

    /**
     * Delay between creating a new leaf and making it visible.
     */
    public static final int LEAF_OPACITY_DELAY = 500;

    protected TreeViewer<V> viewer;

    protected UIComposite   container;

    protected RowLayout     containerLayout;

    /** All curently displayed cells, branches and leafs. */
    protected Map<TreeViewer<V>.Level,UIComponent> cells = new HashMap<>();


    @Override
    @SuppressWarnings( "hiding" )
    public UIComposite init( TreeViewer<V> viewer ) {
        this.viewer = viewer;
        if (!viewer.exclusive.$()) {
            LOG.warn( "TreeViewer.exclusive=true is mandatory for %s", getClass().getSimpleName() );
            viewer.exclusive.set( true );
        }
        return container = new UIComposite() {{
            containerLayout = RowLayout.verticals().fillWidth( true ).spacing( viewer.spacing.$() );
            layout.set( containerLayout );
        }};
    }


    @Override
    public void update( TreeViewer<V>.Changes changes ) {
        var _cells = new HashMap<TreeViewer<V>.Level,UIComponent>();
        var ordered = new ArrayList<UIComponent>();

        // branches
        var branch = viewer.root;
        var branchCount = 0;
        while (true) {
            if (branch.value != null) { // root is not visible
                var cell = getOrCreateCell( branchCount++, -1, branch );
                _cells.put( branch, cell );
                ordered.add( cell );

                if (cell instanceof TreeViewer.ExpandableCell && changes.toggled.contains( branch )) {
                    var _branch = branch;
                    Platform.schedule( BRANCH_UPDATE_EXPAND_DELAY, () -> {
                        if (_branch.isExpanded && !cell.isDisposed()) {
                            ((TreeViewer.ExpandableCell)cell).updateExpand( true );
                            ((UIComposite)cell).layout();
                        }
                    });
                }
            }

            Assert.that( branch.expandedChildren().count() <= 1 );
            var expanded = branch.expandedChildren().first().orNull();
            if (expanded == null) {
                break;
            }
            else {
                branch = expanded;
            }
        }
        Assert.that( branch.isExpanded );

        // leafs
        var leafCount = 0;
        for (var leaf : branch.children) {
            var cell = cells.get( leaf );
            if (cell == null) {
                var c = cell = createCell( leafCount, leaf.value );
                container.components.add( branchCount + leafCount, cell );

                cell.styles.add( CssStyle.of( "opacity", "0" ) );
                Platform.schedule( LEAF_OPACITY_DELAY, () -> {
                    if (!c.isDisposed()) {
                        c.styles.remove( CssStyle.of( "opacity", "0" ) );
                    }
                });
            }
            if (cell instanceof TreeViewer.ExpandableCell && changes.toggled.contains( leaf )) {
                ((TreeViewer.ExpandableCell)cell).updateExpand( false );
            }
            _cells.put( leaf, cell );
            ordered.add( cell );
            leafCount ++;
        }

        // remove old
        for (var entry : cells.entrySet()) {
            if (!_cells.containsKey( entry.getKey() )) {
                //container.components.remove( entry.getValue() );
                entry.getValue().dispose();
            }
        }

        cells = _cells;
        containerLayout.componentOrderor.set( children -> ordered );
        container.layout();
    }


    protected UIComponent getOrCreateCell( int index, int leafIndex, TreeViewer<V>.Level l ) {
        var cell = cells.get( l );
        if (cell == null) {
            cell = createCell( leafIndex, l.value );
            container.components.add( index, cell );
        }
        return cell;
    }

    protected UIComponent createCell( int index, V value ) {
        var cell = viewer.cellBuilder.$().buildCell( index, value, null, viewer );
        cell.cssClasses.add( "TableCell" );

        if (viewer.lines.$()) {
            cell.cssClasses.add( "Lines" );
        }
        //        if (viewer.onSelect.opt().isPresent()) {
        //            result.cssClasses.add( "Clickable" );
        //            result.events.on( EventType.SELECT, ev -> onSelect.$().accept( v ) );
        //        }
        return cell;
    }


    @Override
    public void dispose() {
        if (container != null && !container.isDisposed()) {
            container.dispose();
        }
    }

}
