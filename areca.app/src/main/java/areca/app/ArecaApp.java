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
import areca.app.service.SyncableService;
import areca.app.service.carddav.CarddavService;
import areca.app.service.imap.ImapService;
import areca.app.ui.StartPage;
import areca.common.Platform;
import areca.common.ProgressMonitor;
import areca.common.base.Consumer.RConsumer;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.rt.teavm.ui.UIComponentRenderer;
import areca.ui.App;
import areca.ui.Size;
import areca.ui.component2.Progress;
import areca.ui.component2.Text;
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

    private UIComposite             progressBody;

//    private Progress                progress;
//
//    private Text                    progressText;


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
                layoutConstraints.set( new RowConstraints() {{height.set( rootWindow.size.value().height() - 23 );}} );
            }});

            // monitorBody
            progressBody = rootWindow.add( new UIComposite() {{
                layoutConstraints.set( new RowConstraints() {{height.set( 23 );}} );
                cssClasses.add( "ProgressContainer" );
                layout.set( new RowLayout() {{margins.set( Size.of( 5, 5 ) ); spacing.set( 10 ); fillWidth.set( true ); fillHeight.set( true );}} );

//                add( new Progress() {{
//                    value.set( 0.8f );
//                }});
//                add( new Text() {{ content.set( "80%" ); }} );
            }});
            rootWindow.layout();

            Pageflow.start( mainBody ).open( new StartPage(), null, null );
        });
    }


    public RConsumer<Throwable> defaultErrorHandler() {
        return (Throwable e) -> {
            if (e instanceof ProgressMonitor.CancelledException) {
                LOG.info( "Operation cancelled." );
            }
            else {
                // get a meaningfull stracktrace in TeaVM
                throw (RuntimeException)e;
            }
        };
    }


    public ProgressMonitor newAsyncOperation() {
        return new ProgressMonitor() {
            private Progress progress = progressBody.add( new Progress() );
            private Text progressText = progressBody.add( new Text() {{ content.set( "..." ); }} );
            private long lastUpdate;
            private int workTotal, workDone;
            private String taskName = "";
            private String subTaskName = "";

            @Override
            public void beginTask( String name, int totalWork ) {
                taskName = name;
                progress.max.set( (float)(workTotal = totalWork) );
                progressText.content.set( name );
                progressBody.layout();
            }

            @Override
            public void subTask( String name ) {
                subTaskName = name;
            }

            @Override
            public void worked( int work ) {
                checkCancelled();

                long now = System.currentTimeMillis();
                if (true || now > lastUpdate + 100) {
                    lastUpdate = now;
                    progress.value.set( (float)(workDone += work) );

                    StringBuilder label = new StringBuilder( 64 ).append( taskName );
                    if (!subTaskName.isEmpty()) {
                        label.append( " - " ).append( subTaskName );
                    }
                    label.append( " " ).append( (100/workTotal)*workDone ).append( "%" );
                    progressText.content.set( label.toString() );
                }
            }

            @Override
            public void done() {
                progress.value.set( (float)workTotal );
                progressText.content.set( taskName + " 100%" );
                Platform.schedule( 3000, () -> {
                    progress.dispose();
                    progressText.dispose();
                    progressBody.layout();
                });
            }
        };
    }


    @SuppressWarnings("unchecked")
    public <R> Sequence<R,RuntimeException> services( Class<R> type ) {
        return Sequence.of( services )
                .filter( s -> type.isInstance( s ) )
                .map( s -> (R)s );
    }


    public void startGlobalServicesSync() {
        for (var service : services( SyncableService.class ).asIterable()) {
            var monitor = newAsyncOperation();
            service.newSync( monitor ).start();
        }
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
