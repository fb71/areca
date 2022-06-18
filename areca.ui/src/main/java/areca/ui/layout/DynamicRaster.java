/*
 * Copyright (C) 2022, the @authors. All rights reserved.
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
package areca.ui.layout;

import java.util.List;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Position;
import areca.ui.Size;
import areca.ui.component2.Property;
import areca.ui.component2.Property.ReadWrite;
import areca.ui.component2.UIComposite;

/**
 *
 * @author Falko Bräutigam
 */
public class DynamicRaster
        extends DynamicLayoutManager<DynamicLayoutManager.Component> {

    private static final Log LOG = LogFactory.getLog( DynamicRaster.class );

    public ReadWrite<DynamicRaster,Integer> spacing = Property.rw( this, "spacing", 0 );

    public ReadWrite<DynamicRaster,Size>    margins = Property.rw( this, "margins", Size.of( 0, 0 ) );

    public ReadWrite<DynamicRaster,Size>    itemSize = Property.rw( this, "itemSize" );

    protected Size size;

    protected int cols;

    //protected Map<Integer,Component>        components = new TreeMap<>();


    @Override
    public void layout( UIComposite composite ) {
        super.layout( composite );

        LOG.info( "layout(): %s", composite.clientSize.opt().orElse( Size.of( -1, -1 ) ) );
        if (composite.clientSize.opt().isAbsent()) {
            return;
        }
        size = composite.clientSize.$().substract( margins.value() );
        cols = size.width() / (itemSize.value().width() + spacing.value());

        scrollable.scrollTop.onChange( (newValue,__) -> {
            checkScroll();
        });
        checkScroll();
    }


    protected void checkScroll() {
        LOG.info( "checkScroll(): scroll=%s, height=%s", scrollable.scrollTop.$(), size.height() );
        int scrollTop = scrollable.scrollTop.value() - margins.$().height();
        int skipLines = scrollTop / (itemSize.value().height() + spacing.$());
        int startIndex = skipLines * cols;
        int visibleLines = size.height() / (itemSize.value().height() + spacing.$()) + 1;
        int num = (int)(visibleLines * 1.5) * cols;
        //int startTop = margins.$().height() + (skipLines * (itemSize.value().height() + spacing.$()));
        LOG.info( "checkScroll(): skipLines=%d, startIndex=%d, num=%d", skipLines, startIndex, num );

        ensureComponents( startIndex, num )
                .onSuccess( components -> doLayout( components ) );

//        // check already loaded
//        var loadStartIndex = Sequence.ofInts( startIndex, startIndex+num-1 )
//                .reduce( startIndex, (r,next) -> components.containsKey( next ) ? next+1 : r );
//        var loadNum = num - (loadStartIndex - startIndex);
//        LOG.info( "checkScroll(): loadIndex=%d, loadNum=%d", loadStartIndex, loadNum );
//
//        // load components
//        provider.$().provide( loadStartIndex, loadNum ).onSuccess( newComponents -> {
//            for (var c : newComponents) {
//                if (components.putIfAbsent( c.index, c ) == null) {
//                    scrollable.add( c.component );
//                } else {
//                    c.component.dispose();
//                }
//            }
//            doLayout( loadStartIndex, loadNum, startTop );
//        });
    }


    protected void doLayout( List<Component> components ) {
        var cWidth = itemSize.value().width();
        var cHeight = itemSize.value().height();

        for (var c : components) {
            var col = c.index % cols;
            var line = c.index / cols;
            var child = c.component;
            child.size.set( Size.of( cWidth, cHeight ) );
            child.position.set( Position.of(
                    margins.value().width() + ((cWidth + spacing.value()) * col),
                    margins.value().height() + ((cHeight + spacing.value()) * line) ) );
        }
    }


    @Override
    public void componentHasChanged( Component changed ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

}
