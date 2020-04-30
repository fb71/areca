/*
 * Copyright (C) 2020, the @authors. All rights reserved.
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

import java.util.logging.Logger;

import areca.rt.teavm.ui.TeaApp;
import areca.ui.Button;
import areca.ui.SelectionEvent;
import areca.ui.layout.FillLayout;

/**
 *
 * @author Falko Bräutigam
 */
public class Main {

    private static final Logger LOG = Logger.getLogger( Main.class.getName() );


    public static void main( String[] args ) throws Exception {
        try {
            TeaApp.instance().createUI( appWindow -> {
                appWindow.layoutManager.set( new FillLayout() );

                // Button1
                appWindow.create( Button.class, btn1 -> {
                    btn1.label.set( "Button! --- !" );
                    btn1.layoutConstraints.get().clear();
                    btn1.subscribe( (SelectionEvent ev) -> {
                        LOG.info( "clicked: " + ev ); // ev.getType() + ", ctrl=" + ev.getCtrlKey() + ", pos=" + ev.getClientX() + "/" + ev.getClientY() );
                    });
                });
            })
            //           // Button2
            //           .create( Button.class, self -> {
            //               self.label.set( "Button2" );
            //               self.bgColor.set( Color.WHITE );
            //               self.subscribe( (SelectionEvent ev) -> {
            //                   LOG.info( "" + ev );
            //               });
            //           })
            .layout();
        }
        catch (Exception e) {
            System.out.println( e.getMessage() + " (" + e.getClass().getName() + ")" );
            throw e;
        }
    }

}
