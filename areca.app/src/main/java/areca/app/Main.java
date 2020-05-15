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

import java.util.Arrays;
import java.util.logging.Logger;

import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.store.tidbstore.IDBStore;

import areca.app.model.Anchor;
import areca.common.base.Lazy;
import areca.rt.teavm.ui.TeaApp;
import areca.ui.Position;
import areca.ui.Size;
import areca.ui.component.Button;
import areca.ui.component.SelectionEvent;
import areca.ui.layout.FillLayout;
import areca.ui.viewer.LabeledList;

/**
 *
 * @author Falko Bräutigam
 */
public class Main {

    private static final Logger LOG = Logger.getLogger( Main.class.getName() );

    private static Lazy<EntityRepository,Exception> repo;

    static {
        repo = new Lazy<>( () -> {
            LOG.info( "MAIN: creating repo..." );
            return EntityRepository.newConfiguration()
                    .entities.set( Arrays.asList( Anchor.info ) )
                    .store.set( new IDBStore( "main", 1 ) )
                    .create();
        });
    }

    public static void main( String[] args ) throws Exception {
        try {
            TeaApp.instance().createUI( appWindow -> {
                appWindow.size.set( Size.of( 400, 300 ) );
                appWindow.layout.set( new FillLayout() );

                // Button1
                appWindow.add( new Button(), btn -> {
                    btn.label.set( "Button!" );
                    btn.subscribe( (SelectionEvent ev) -> {
                        LOG.info( "clicked: " + ev ); // ev.getType() + ", ctrl=" + ev.getCtrlKey() + ", pos=" + ev.getClientX() + "/" + ev.getClientY() );
                        Position pos = btn.position.get();
                        btn.position.set( Position.of( pos.x()-10, pos.y()-10 ) );
                    });
//                    btn.size.set( Size.of( 100, 100 ) );
                    btn.position.set( Position.of( 100, 100 ) );
                });

                appWindow.add( new LabeledList<String>(), l -> {
                    l.firstLineLabeler.set( data -> ":: " + data );
                    l.setData( 0, new String[] {"1", "2", "3", "4", "5", "6", "7"} );
                });
//                Thread.sleep( 100 );
            })
            .layout();
        }
        catch (Throwable e) {
            System.out.println( "Exception: " + e + " --> " );
            Throwable rootCause = e;
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }
            System.out.println( "Root cause: " + rootCause );
            throw (Exception)rootCause;
        }
    }

}
