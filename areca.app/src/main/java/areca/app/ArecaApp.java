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

import static areca.ui.Orientation.VERTICAL;
import static java.util.Arrays.asList;

import java.util.Arrays;
import java.util.List;

import org.teavm.jso.browser.Window;

import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.store.tidbstore.IDBStore;

import areca.app.model.Anchor;
import areca.app.model.Contact;
import areca.app.model.Message;
import areca.app.service.Service;
import areca.app.service.carddav.CarddavService;
import areca.app.service.imap.ImapService;
import areca.app.ui.StartPage;
import areca.common.ProgressMonitor;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.rt.teavm.ui.UIComponentRenderer;
import areca.ui.App;
import areca.ui.Color;
import areca.ui.Size;
import areca.ui.component2.UIComposite;
import areca.ui.component2.VisualActionFeedback;
import areca.ui.layout.RowConstraints;
import areca.ui.layout.RowLayout;
import areca.ui.pageflow.Pageflow;

/**
 *
 * @author Falko Bräutigam
 */
public class ArecaApp extends App {

    private static final Log LOG = LogFactory.getLog( ArecaApp.class );

    public static ArecaApp instance() {
        return instance != null ? (ArecaApp)instance : (ArecaApp)(instance = new ArecaApp());
    }

    // instance *******************************************

    private List<? extends Service> services = Arrays.asList(  // XXX from DB?
            new CarddavService(),
            new ImapService() );

    private EntityRepository        repo;

    private UnitOfWork              uow;

    private UIComposite             mainBody;

    private UIComposite             monitorBody;


    protected ArecaApp() {
        EntityRepository.newConfiguration()
                .entities.set( asList( Message.info, Contact.info, Anchor.info) )
                .store.set( new IDBStore( "areca.app", 1, false ) )
                .create()
                .onSuccess( result -> {
                    repo = result;
                    LOG.info( "Database repo initialized." );
                });
    }


    public void createUI() {
        UIComponentRenderer.start();

        super.createUI( rootWindow -> {
            VisualActionFeedback.start();
            // XXX
            rootWindow.size.defaultsTo( () -> {
                var doc = Window.current().getDocument();
                var size = Size.of( doc.getBody().getClientWidth(), doc.getBody().getClientHeight() );
                LOG.info( "BODY: " + size );
                return size;
            });

            rootWindow.layout.set( new RowLayout() {{orientation.set( VERTICAL ); fillWidth.set( true );}} );
            mainBody = rootWindow.add( new UIComposite() {{
                layoutConstraints.set( new RowConstraints() {{height.set( rootWindow.size.value().height() - 25 );}} );
            }});
            monitorBody = rootWindow.add( new UIComposite() {{
                layoutConstraints.set( new RowConstraints() {{height.set( 25 );}} );
                bgColor.set( Color.rgb( 0x17, 0x15, 0x14 ) );
            }});
            rootWindow.layout();

            Pageflow.start( mainBody ).open( new StartPage(), null, null );
        });
    }


    public ProgressMonitor newAsyncOperation() {
        throw new RuntimeException( "not yet..." );

    }


    @SuppressWarnings("unchecked")
    public <R extends Service> Sequence<R,RuntimeException> services( Class<R> type ) {
        return Sequence.of( services )
                .filter( s -> type.isInstance( s ) )
                .map( s -> (R)s );
    }


    public EntityRepository repo() {
        return repo;
    }


    public UnitOfWork unitOfWork() {
        if (uow == null) {
            uow = repo.newUnitOfWork();
        }
        return uow;
    }
}
