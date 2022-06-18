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
package areca.ui.test;

import areca.common.Promise;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.ui.Size;
import areca.ui.component2.Button;
import areca.ui.component2.ScrollableComposite;
import areca.ui.component2.UIComposite;
import areca.ui.layout.DynamicLayoutManager.Component;
import areca.ui.layout.DynamicRaster;
import areca.ui.layout.FillLayout;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class DynamicRasterTest {

    private static final Log LOG = LogFactory.getLog( DynamicRasterTest.class );

    public static final ClassInfo<DynamicRasterTest> info = DynamicRasterTestClassInfo.instance();

    @Test( "Raster" )
    protected static void createRasterTransition( UIComposite parent ) {
        parent.layout.set( new FillLayout() );

        parent.add( new ScrollableComposite() {{
            layout.set( new DynamicRaster()
                    .itemSize.set( Size.of( 80, 75 ) )
                    .provider.set( (start,num) -> {
                        LOG.info( "Provide: %d-%d", start, num );
                        var result = Sequence.ofInts( start, start+num-1 )
                                .map( i -> new Component( new Button().label.set( ""+i ), i ) )
                                .toList();
                        return Promise.completed( result );
                    }));
        }});
        parent.layout();
    }

}
