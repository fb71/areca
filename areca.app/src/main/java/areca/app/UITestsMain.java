/*
 * Copyright (C) 2021, the @authors. All rights reserved.
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

import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.UnitOfWork;

import areca.app.model.Message;
import areca.common.base.Lazy;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.App;
import areca.ui.Position;
import areca.ui.Size;
import areca.ui.component.Button;
import areca.ui.component.SelectionEvent;
import areca.ui.component.Text;
import areca.ui.layout.FillLayout;
import areca.ui.layout.GridLayout;
import areca.ui.viewer.LabeledList;

/**
 *
 * @author Falko Bräutigam
 */
public class UITestsMain {

    private static final Log log = LogFactory.getLog( UITestsMain.class );

    public static Lazy<EntityRepository,RuntimeException>   repo;

    public static Lazy<UnitOfWork,RuntimeException>         uow;


    static {
//        repo = new Lazy<>( () -> {
//            log.info( "creating repo..." );
//            var result = EntityRepository.newConfiguration()
//                    .entities.set( Arrays.asList( Anchor.info, Contact.info, Message.info ) )
//                    .store.set( new IDBStore( "main2", 1, false ) )
//                    .create();
//            log.info( "creating test data..." );
//            TestDataBuilder.run( result );
//            return result;
//        });
//        uow = new Lazy<>( () -> repo.supply().newUnitOfWork() );
    }


    public static void createMessagesListApp() {
        App.instance().createUI( appWindow -> {
            appWindow.size.set( Size.of( 400, 300 ) );
            appWindow.layout.set( new FillLayout() );

            // Button1
            appWindow.add( new Button(), btn -> {
                btn.label.set( "Button!" );
                btn.onClick( (SelectionEvent ev) -> {
                    log.info( "clicked: " + ev ); // ev.getType() + ", ctrl=" + ev.getCtrlKey() + ", pos=" + ev.getClientX() + "/" + ev.getClientY() );
                    Position pos = btn.position.get();
                    btn.position.set( Position.of( pos.x()-10, pos.y()-10 ) );
                });
                btn.position.set( Position.of( 100, 100 ) );
            });

            // Messages list
            appWindow.add( new LabeledList<Message>(), l -> {
                l.firstLineLabeler.set( data -> data.from.get() );
                l.setData( 0, uow.supply().query( Message.class ).execute() );
            });
        })
        .layout();
    }


    public static void createGridLayoutApp() {
        App.instance().createUI( appWindow -> {
            //appWindow.size.set( Size.of( 400, 300 ) );
            appWindow.layout.set( new GridLayout() {{spacing.set( 10 );}} );

            appWindow.add( new Text(), text -> text.text.set( "Samstagabend" ) );

            for (int i = 0; i < 2; i++) {
                var label = "" + i;
                appWindow.add( new Button(), btn -> {
                    btn.label.set( label );
                    btn.onClick( ev ->  {
                        appWindow.layout.set( (appWindow.layout.get() instanceof FillLayout)
                                ? new GridLayout() : new FillLayout() );
                        appWindow.layout();
                    });
                });
            }
        })
        .layout();
    }

//  var div = new HtmlElement( Type.DIV );
//  var btn = div.children.add( new HtmlButton() );
//  // btn.styles.set( "background-color", Color.WHITE );
//  btn.styles.set( "width", "50px" );
//  btn.styles.set( "height", "%spx", 50 );
//  btn.styles.set( "position", "absolute" );
//  btn.styles.set( "top", "%spx", 50 );
//  btn.styles.set( "left", "%spx", 50 );
//  log.info( "client= %s", btn.clientSize.get() );
//  log.info( "offset= %s", btn.offsetSize.get() );
//  log.info( "position= %s", btn.offsetPosition.get() );
//  var handle = btn.listeners.click( ev -> {
//      log.info( "position= %s", ev.clientPosition.get() );
//  });
//  btn.listeners.remove( handle );
//  btn.listeners.mouseMove( ev -> {
//      btn.styles.set( "left", "%spx", ev.clientPosition.get().x() );
//  });
//
// // btn.children.append( new TextNode() );


//  protected static void createControllerView() {
//  TeaApp.instance().createUI( appWindow -> {
//      ControllerSiteImpl site = new ControllerSiteImpl( new MessageController() );
//      Message message = uow.get().query( Message.class ).execute().iterator().next();
//      site.addContext( message, Mode.IN_OUT, "" );
//      site.createUI( appWindow );
//  })
//  .layout();
//}

}
