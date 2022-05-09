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
package areca.ui.test;

import static areca.ui.component2.Events.EventType.SELECT;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.App;
import areca.ui.component2.Button;
import areca.ui.component2.Text;
import areca.ui.layout.FillLayout;
import areca.ui.layout.RasterLayout;

/**
 *
 * @author Falko Bräutigam
 */
public class ViewersGallery {

    private static final Log log = LogFactory.getLog( ViewersGallery.class );

//    public static Lazy<EntityRepository,RuntimeException>   repo;
//
//    public static Lazy<UnitOfWork,RuntimeException>         uow;


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


//    public static void createMessagesList( UIComposite parent ) {
//        parent.layout.set( new FillLayout() );
//
//        // Button1
//        parent.add( new Button(), btn -> {
//            btn.label.set( "Button!" );
//            btn.events.on( EventType.SELECT, ev -> {
//                log.info( "clicked: " + ev ); // ev.getType() + ", ctrl=" + ev.getCtrlKey() + ", pos=" + ev.getClientX() + "/" + ev.getClientY() );
//                Position pos = btn.position.value();
//                btn.position.set( Position.of( pos.x()-10, pos.y()-10 ) );
//            });
//            btn.position.set( Position.of( 100, 100 ) );
//        });
//
//        // Messages list
//        parent.add( new LabeledList<Message>(), l -> {
//            l.firstLineLabeler.set( data -> data.from.get() );
//            uow.supply().query( Message.class )
//            .execute()
//            //.reduce( new ArrayList<Message> )
//            .onSuccess( (self,opt) -> {
//                opt.ifPresent( m -> l.setData( self.index(), Collections.singleton( m ) ) );
//            });
//        });
//        parent.layout();
//    }


    public static void createGridLayoutApp() {
        App.instance().createUI( appWindow -> {
            //appWindow.size.set( Size.of( 400, 300 ) );
            appWindow.layout.set( new RasterLayout() {{spacing.set( 10 );}} );

            appWindow.add( new Text(), text -> text.content.set( "Samstagabend" ) );

            for (int i = 0; i < 2; i++) {
                var label = "" + i;
                appWindow.add( new Button(), btn -> {
                    btn.label.set( label );
                    btn.events.on( SELECT, ev ->  {
                        appWindow.layout.set( (appWindow.layout.value() instanceof FillLayout)
                                ? new RasterLayout() : new FillLayout() );
                        appWindow.layout();
                    });
                });
            }
        })
        .layout();
    }

}
