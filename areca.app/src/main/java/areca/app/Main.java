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

import org.teavm.jso.browser.Location;
import org.teavm.jso.browser.Window;

import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.store.tidbstore.IDBStore;

import areca.app.model.Anchor;
import areca.app.model.Contact;
import areca.app.model.Message;
import areca.common.base.Lazy;
import areca.common.event.EventManager;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.rt.teavm.SetTimeoutEventManager;
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

    private static final Log log = LogFactory.getLog( Main.class );

    public static Lazy<EntityRepository,RuntimeException>   repo;

    public static Lazy<UnitOfWork,RuntimeException>         uow;


    static {
        repo = new Lazy<>( () -> {
            log.info( "creating repo..." );
            EntityRepository result = EntityRepository.newConfiguration()
                    .entities.set( Arrays.asList( Anchor.info, Contact.info, Message.info ) )
                    .store.set( new IDBStore( "main2", 1, false ) )
                    .create();
            log.info( "creating test data..." );
            TestDataBuilder.run( result );
            return result;
        });
        uow = new Lazy<>( () -> repo.supply().newUnitOfWork() );
    }


    public static void main( String[] args ) throws Exception {
        // TestRunner
        Location location = Window.current().getLocation();
        if (location.getHash().equals( "#test" ) || location.getSearch().contains( "test=true" )) {
            TestRunnerMain.main( args );
            log.info( "done." );
            return;
        }
        // App
        try {
            EventManager.setInstance( new SetTimeoutEventManager() );
            log.info( "EventManager: " + EventManager.instance().getClass().getSimpleName() );

            log.info( "Repo: " + repo.supply() );
            System.out.println( "Hallo" );

            TeaApp.instance().createUI( appWindow -> {
                appWindow.size.set( Size.of( 400, 300 ) );
                appWindow.layout.set( new FillLayout() );

//                appWindow.add( new Button() {{
//                    label.set( "Initializer" );
//                }});

                // Button1
                appWindow.add( new Button(), btn -> {
                    btn.label.set( "Button!" );
                    btn.subscribe( (SelectionEvent ev) -> {
                        log.info( "clicked: " + ev ); // ev.getType() + ", ctrl=" + ev.getCtrlKey() + ", pos=" + ev.getClientX() + "/" + ev.getClientY() );
                        Position pos = btn.position.get();
                        btn.position.set( Position.of( pos.x()-10, pos.y()-10 ) );
                    });
//                    btn.size.set( Size.of( 100, 100 ) );
                    btn.position.set( Position.of( 100, 100 ) );
                });

                // Messages list
                appWindow.add( new LabeledList<Message>(), l -> {
                    l.firstLineLabeler.set( data -> data.from.get() );
                    l.setData( 0, uow.supply().query( Message.class ).execute() );
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
