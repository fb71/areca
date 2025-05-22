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
import areca.common.MutableInt;
import areca.common.Platform;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.layout.RowLayout;
import areca.ui.viewer.TreeViewer.TreeViewerLayout;

/**
 * A {@link TreeViewer} layout that shows just the one, expanded branch of the tree.
 *
 * @author Falko Bräutigam
 */
public class DrillingTreeLayout<V>
        extends TreeViewerLayout<V> {

    private static final Log LOG = LogFactory.getLog( DrillingTreeLayout.class );

    private TreeViewer<V>       viewer;

    private UIComposite         container;

    private Map<V,UIComponent>  topChildren = new HashMap<>();

    private Map<TreeViewer<V>.Level,UIComponent> expanded = new HashMap<>();


    @Override
    public UIComposite init( @SuppressWarnings( "hiding" ) TreeViewer<V> viewer ) {
        this.viewer = viewer;
        return new UIComposite() {{
            container = this;
            layout.set( RowLayout.verticals().fillWidth( true ).spacing( viewer.spacing.$() ) );
        }};
    }


    @Override
    public void expand( TreeViewer<V>.Level level ) {
        // remove top-children
        int delay = 0;
        if (!topChildren.isEmpty()) { // root?
            var c = topChildren.remove( level.value );
            expanded.put( level, Assert.notNull( c, "Expand level is not in current top-children: " + level.value ) );

            topChildren.values().forEach( UIComponent::dispose );
            topChildren.clear();

            container.layout_();
            delay = 350;
        }
        // create new top-children
        Platform.schedule( delay, () -> { // wait for animation
            int i = 0;
            for (var l : level.children) {
                var cell = createCell( i++, l.value );
                topChildren.put( l.value, cell );
                container.add( cell );

                if (viewer.oddEven.$()) {
                    cell.cssClasses.modify( "Even", i % 2 == 0 );
                }
            }
            container.layout();
        });
    }


    @Override
    public void collapse( TreeViewer<V>.Level level ) {
        Assert.that( !topChildren.isEmpty() );
        topChildren.values().forEach( UIComponent::dispose );
        topChildren.clear();

        var c = expanded.remove( level );
        topChildren.put( level.value, c );

        update( level.parent );
    }


    @Override
    public void update( TreeViewer<V>.Level level ) {
        // remove
        var newChildren = Sequence.of( level.children ).map( c -> c.value ).toSet();
        for (var v : new ArrayList<>( topChildren.keySet() )) {
            if (!newChildren.contains( v )) {
                topChildren.remove( v ).dispose();
            }
        }
        // add new
        var i = new MutableInt();
        for (var l : level.children) {
            topChildren.computeIfAbsent( l.value, __ -> {
                var cell = createCell( i.getValue(), l.value );
                container.components.add( expanded.size() + i.intValue(), cell );
                return cell;
            });
            i.incrementAndGet();
        }
        container.layout();
    }


    protected UIComponent createCell( int index, V value ) {
            var result = viewer.cellBuilder.$().buildCell( index, value, null, viewer );
            result.cssClasses.add( "TableCell" );

            if (viewer.lines.$()) {
                result.cssClasses.add( "Lines" );
            }
            //        if (viewer.onSelect.opt().isPresent()) {
            //            result.cssClasses.add( "Clickable" );
            //            result.events.on( EventType.SELECT, ev -> onSelect.$().accept( v ) );
            //        }
            return result;
    }

}
