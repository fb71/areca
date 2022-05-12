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

import static areca.ui.Orientation.VERTICAL;
import static areca.ui.component2.Events.EventType.SELECT;

import java.util.ArrayList;
import java.util.List;

import java.lang.reflect.InvocationTargetException;

import areca.common.base.Consumer;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Level;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.MethodInfo;
import areca.ui.Size;
import areca.ui.component2.Button;
import areca.ui.component2.UIComposite;
import areca.ui.component2.VisualActionFeedback;
import areca.ui.layout.RowConstraints;
import areca.ui.layout.RowLayout;

/**
 *
 * @author Falko Bräutigam
 */
public class GalleryMain {

    private static final Log LOG = LogFactory.getLog( GalleryMain.class );

    public static void initLog() {
        LogFactory.DEFAULT_LEVEL = Level.INFO;
        LogFactory.setPackageLevel( areca.ui.component2.UIComponent.class, Level.DEBUG );
        LogFactory.setPackageLevel( GalleryMain.class, Level.DEBUG );
        //LogFactory.setPackageLevel( areca.rt.teavm.ui.UIComponentRenderer.class, Level.DEBUG );
    }

    public static final List<Class<?>> TESTS = new ArrayList<>() {{
        add( RowLayoutTest.class );
        add( RasterLayoutTest.class );
    }};

    protected static UIComposite    btnArea;

    protected static UIComposite    contentArea;


    public static UIComposite createApp( Size appSize ) {
        initLog();
        VisualActionFeedback.start();

        return createUI( appWindow -> {
            appWindow.size.set( appSize );
            appWindow.layout.set( new RowLayout() {{orientation.set( VERTICAL ); fillWidth.set( true ); margins.set( Size.of( 3, 3 ) );}} );
            //appWindow.bordered.set( true );

            // buttons
            btnArea = appWindow.add( new UIComposite() {{
                layoutConstraints.set( new RowConstraints() {{height.set( 40 );}} );
                layout.set( new RowLayout() {{fillWidth.set( true ); fillHeight.set( true ); spacing.set( 3 ); margins.set( Size.of( 0, 3 ) );}} );

                for (Class<?> testClass : TESTS) {
                    for (MethodInfo m : ClassInfo.of( testClass ).methods()) {
                        m.annotation( Test.class ).ifPresent( a -> {
                            add( new Button() {{
                                label.set( a.value() );
                                events.on( SELECT, ev -> {
                                    try {
                                        var test = ClassInfo.of( testClass ).newInstance();
                                        m.invoke( test, clear( contentArea ) );
                                    }
                                    catch (InvocationTargetException e) {
                                        throw (RuntimeException)e.getCause();
                                    }
                                    catch (Exception e) {
                                        throw (RuntimeException)e;
                                    }
                                });
                            }});
                        });
                    }
                }
            }});

            // content
            contentArea = appWindow.add( new UIComposite() {{
                layoutConstraints.set( new RowConstraints() {{height.set( appWindow.size.value().height()-40 );}} );
                bordered.set( true );
            }});

            appWindow.layout();
        })
        .layout();
    }


    protected static UIComposite clear( UIComposite composite ) {
        //composite.layout.set( new FillLayout() );
        composite.components.disposeAll();
        return composite;
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
