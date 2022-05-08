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
package areca.app;

import areca.common.base.Consumer;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Level;
import areca.common.log.LogFactory.Log;
import areca.rt.teavm.ui.UIComponentRenderer;
import areca.ui.Color;
import areca.ui.Orientation;
import areca.ui.Size;
import areca.ui.component2.Button;
import areca.ui.component2.Events.EventType;
import areca.ui.component2.Text;
import areca.ui.component2.UIComposite;
import areca.ui.layout.FillLayout;
import areca.ui.layout.RowLayout;

/**
 *
 * @author Falko Bräutigam
 */
public class UIComponentGalleryMain {

    private static final Log LOG = LogFactory.getLog( UIComponentGalleryMain.class );

    public static void initLog() {
        LogFactory.DEFAULT_LEVEL = Level.INFO;
        LogFactory.setPackageLevel( areca.ui.component2.UIComponent.class, Level.DEBUG );
        LogFactory.setPackageLevel( areca.rt.teavm.ui.UIComponentRenderer.class, Level.DEBUG );
    }


    protected static UIComposite    contentArea;

    public static void createApp() {
        initLog();
        UIComponentRenderer.start();

        createUI( appWindow -> {
            appWindow.size.set( Size.of( 400, 400 ) );
            appWindow.layout.set( new FillLayout() );
            appWindow.bordered.set( true );
            createFillLayout( appWindow );
            appWindow.layout();
        })
        .layout();
    }


    protected static void clear( UIComposite parent) {
        parent.layout.set( new FillLayout() );
        parent.components.disposeAll();
    }


    protected static void createFillLayout( UIComposite parent ) {
        var layout = new RowLayout() {{margins.set( Size.of( 10, 10 ) ); spacing.set( 10 ); fillWidth.set( true );}};
        parent.layout.set( layout );

        parent.add( new Text() {{
            content.set( "UI2 :) -- " + bordered );
            bordered.set( true );
            events.on( EventType.SELECT, ev -> {
                LOG.info( "CLICK: " + ev );
                var current = size.value();
                size.set( Size.of( current.width() - 20, current.height() - 20 ) );
                bgColor.set( Color.rgb( 0x30, 0x40, 0x50 ) );
                layout.orientation.set( Orientation.VERTICAL );
                parent.layout();
            });
        }});

        parent.add( new Button() {{
            LOG.warn( "CSS: " + cssClasses );
            label.set( "+++" );
            events.on( EventType.SELECT, ev -> {
                this.label.set( "Hola!" );
                // Platform.schedule( 1000, () -> this.dispose() );
                layout.orientation.set( Orientation.HORIZONTAL );
                parent.layout();
            });
        }});
    }


    protected static void createRasterTransition() {
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
//  }

    }


    protected static <E extends Exception> UIComposite createUI( Consumer<UIComposite,E> initializer ) throws E {
        RootWindow rootWindow = new RootWindow();
        initializer.accept( rootWindow );
        return rootWindow;
    }


    /**
     *
     */
    public static class RootWindow
            extends UIComposite {
    }

}
