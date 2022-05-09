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

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.ui.component2.Text;
import areca.ui.component2.UIComposite;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class RasterLayoutTest {

    private static final Log LOG = LogFactory.getLog( RasterLayoutTest.class );

    public static final ClassInfo<RasterLayoutTest> info = RasterLayoutTestClassInfo.instance();

    @Test( "Raster" )
    protected static void createRasterTransition( UIComposite parent ) {
        parent.add( new Text() {{content.set( "Raster..." );}} );
//      for (int i = 0; i < 2; i++) {
//      var label = "" + i;
//      appWindow.add( new Button(), btn -> {
//          btn.label.set( label );
//          btn.events.onSelection( ev ->  {
//              appWindow.layout.set( (appWindow.layout.get() instanceof FillLayout)
//                      ? new RasterLayout() : new FillLayout() );
//              appWindow.layout();
//          });
//      });
    }



}
