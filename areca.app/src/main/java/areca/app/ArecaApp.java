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
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.Arrays;
import java.util.List;

import org.teavm.jso.browser.Window;

import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.Lifecycle.State;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.store.tidbstore.IDBStore;

import areca.app.model.Anchor;
import areca.app.model.Contact;
import areca.app.model.EntityLifecycleEvent;
import areca.app.model.ImapSettings;
import areca.app.model.MatrixSettings;
import areca.app.model.Message;
import areca.app.model.ModelSubmittedEvent;
import areca.app.model.SmtpSettings;
import areca.app.service.Service;
import areca.app.service.SyncableService;
import areca.app.service.SyncableService.SyncContext;
import areca.app.service.SyncableService.SyncType;
import areca.app.service.carddav.CarddavService;
import areca.app.service.matrix.MatrixService;
import areca.app.ui.StartPage;
import areca.common.Assert;
import areca.common.Platform;
import areca.common.ProgressMonitor;
import areca.common.Promise;
import areca.common.Timer;
import areca.common.WaitFor;
import areca.common.base.Consumer.RConsumer;
import areca.common.base.Sequence;
import areca.common.base.With;
import areca.common.event.EventCollector;
import areca.common.event.EventManager;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.rt.teavm.ui.UIComponentRenderer;
import areca.ui.App;
import areca.ui.Size;
import areca.ui.component2.Progress;
import areca.ui.component2.Text;
import areca.ui.component2.UIComposite;
import areca.ui.layout.RowConstraints;
import areca.ui.layout.RowLayout;
import areca.ui.pageflow.Pageflow;

/**
 *
 * @author Falko Bräutigam
 */
public class ArecaApp extends App {

    static final Log LOG = LogFactory.getLog( ArecaApp.class );

    public static ArecaApp instance() {
        return instance != null ? (ArecaApp)instance : (ArecaApp)(instance = new ArecaApp());
    }

    public static String proxiedUrl( String url ) {
        return "http?uri=" + url;
    }

    // instance *******************************************

    private List<? extends Service> services = Arrays.asList(  // XXX from DB?
            new CarddavService(),
            //new ImapService(),
            new MatrixService() );

    private EntityRepository        repo;

    private UnitOfWork              uow;

    private EntityRepository        settingsRepo;

    private UnitOfWork              settingsUow;

    private UIComposite             mainBody;

    private UIComposite             progressBody;


    protected ArecaApp() {
        var appEntityTypes = asList( Message.info, Contact.info, Anchor.info);
        var appEntities = Sequence.of( appEntityTypes ).map( info -> info.type() ).toList();
        EntityRepository.newConfiguration()
                .entities.set( appEntityTypes )
                .store.set( new IDBStore( "areca.app", 10, true ) )
                .create()
                .onSuccess( result -> {
                    repo = result;
                    uow = repo.newUnitOfWork();
                    LOG.info( "Database and model repo initialized." );
                });

        var settingsEntityTypes = asList( ImapSettings.info, MatrixSettings.info, SmtpSettings.info );
        var settingsEntities = Sequence.of( settingsEntityTypes ).map( info -> info.type() ).toList();
        EntityRepository.newConfiguration()
                .entities.set( settingsEntityTypes )
                .store.set( new IDBStore( "areca.app.settings", 3, true ) )
                .create()
                .onSuccess( result -> {
                    settingsRepo = result;
                    settingsUow = settingsRepo.newUnitOfWork();
                    LOG.info( "Settings database and model repo initialized." );
                });

        // app model updates
        var collector = new EventCollector<EntityLifecycleEvent>( 100 );
        EventManager.instance()
                .subscribe( (EntityLifecycleEvent ev) -> {
                    // no refresh needed if main UoW was submitted
                    if (ev.getSource().context.getUnitOfWork() == uow) {
                        return;
                    }
                    collector.collect( ev, collected -> {
                        var ids = Sequence.of( collected ).map( _ev -> _ev.getSource().id() ).toSet();
                        uow.refresh( ids ).onSuccess( __ -> {
                            EventManager.instance().publish( new ModelSubmittedEvent( this, collected ) );
                        });
                    });
                })
                .performIf( ev -> With.$( ev ).instanceOf( EntityLifecycleEvent.class )
                        .map( lev -> lev.state == State.AFTER_SUBMIT && appEntities.contains( lev.getSource().getClass() ) )
                        .orElse( false ))
                .unsubscribeIf( () -> !uow.isOpen() );

        // settings model updates
        var collector2 = new EventCollector<EntityLifecycleEvent>( 100 );
        EventManager.instance()
                .subscribe( (EntityLifecycleEvent ev) -> {
                    // no refresh needed if main UoW was submitted
                    if (ev.getSource().context.getUnitOfWork() == settingsUow) {
                        return;
                    }
                    collector2.collect( ev, collected -> {
                        var ids = Sequence.of( collected ).map( _ev -> _ev.getSource().id() ).toSet();
                        settingsUow.refresh( ids ).onSuccess( __ -> {
                            EventManager.instance().publish( new ModelSubmittedEvent( this, collected ) );
                        });
                    });
                })
                .performIf( ev -> With.$( ev ).instanceOf( EntityLifecycleEvent.class )
                        .map( lev -> lev.state == State.AFTER_SUBMIT && settingsEntities.contains( lev.getSource().getClass() ) )
                        .orElse( false ))
                .unsubscribeIf( () -> !settingsUow.isOpen() );
    }


    public void createUI() {
        UIComponentRenderer.start();

        super.createUI( rootWindow -> {
            //VisualActionFeedback.start();

            rootWindow.size.defaultsTo( () -> {
                var doc = Window.current().getDocument();
                var size = Size.of( doc.getBody().getClientWidth(), doc.getBody().getClientHeight() );
                LOG.info( "BODY: " + size );
                return size;
            });

            rootWindow.layout.set( new RowLayout() {{orientation.set( VERTICAL ); fillWidth.set( true );}} );
            mainBody = rootWindow.add( new UIComposite() {{
                layoutConstraints.set( new RowConstraints() {{height.set( rootWindow.size.value().height() - 6 );}} );
            }});

            // monitorBody
            progressBody = rootWindow.add( new UIComposite() {{
                layoutConstraints.set( new RowConstraints() {{height.set( 6 );}} );
                cssClasses.add( "ProgressContainer" );
                layout.set( new RowLayout() {{margins.set( Size.of( 3, 1 ) ); spacing.set( 10 ); fillWidth.set( true ); fillHeight.set( true );}} );

//                add( new Progress() {{
//                    value.set( 0.8f );
//                }});
//                add( new Text() {{ content.set( "80%" ); }} );
            }});
            rootWindow.layout();

            Pageflow.start( mainBody ).open( new StartPage(), null, null );

            // start background sync services (after we have the monitor UI)
            Platform.schedule( 1000, () -> startSync( SyncType.BACKGROUND ) );
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
            private Text progressText = /*progressBody.add(*/ new Text() {{ content.set( "..." ); }}; // );
            private Timer lastUpdate = Timer.start();
            private int workTotal, workDone;
            private String taskName = "";
            private String subTaskName = "";

            @Override
            public void beginTask( String name, int totalWork ) {
                Assert.that( taskName.isEmpty() );
                taskName = name;
                progressText.content.set( name );
                progressBody.layout();
                updateTotalWork( totalWork );
            }

            @Override
            public void updateTotalWork( int toAdd ) {
                Assert.that( !taskName.isEmpty() );
                workTotal += toAdd;
                progress.max.set( (float)(workTotal) );
                LOG.debug( "PROGRESS: update: +%s -> %s", toAdd, workTotal );
            }

            @Override
            public void subTask( String name ) {
                // subTaskName = name;
            }

            @Override
            public void worked( int work ) {
                Assert.that( !taskName.isEmpty() );
                checkCancelled();

                workDone += work;

                if (work > 1 || lastUpdate.elapsed( MILLISECONDS ) > 100) {
                    lastUpdate.restart();
                    progress.value.set( (float)workDone );

                    StringBuilder label = new StringBuilder( 64 ).append( taskName );
                    if (!subTaskName.isEmpty()) {
                        label.append( " - " ).append( subTaskName );
                    }
                    label.append( " " ).append( (100/workTotal)*workDone ).append( "%" );
                    progressText.content.set( label.toString() );
                    LOG.debug( "PROGRESS: work: %s / %s", workDone, workTotal );
                }
            }

            @Override
            public void done() {
                progress.value.set( Float.valueOf( workTotal ) );
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


    public void startSync( SyncType type  ) {
        services( SyncableService.class ).forEach( (service,i) -> {
            Platform.schedule( 3000 * i, () -> { // XXX start imap after contacts are there
                var ctx = new SyncContext() {{
                    monitor = newAsyncOperation();
                    uowFactory = () -> repo().newUnitOfWork();
                }};
                service.newSync( type, ctx )
                        .onSuccess( sync -> {
                            if (sync != null) {
                                sync.start()
                                        .onSuccess( __ -> ctx.monitor.done() )
                                        .onError( defaultErrorHandler() );
                            }
                            else {
                                LOG.info( "%s: nothing to sync or no settings.", service.getClass().getSimpleName() );
                                ctx.monitor.done();
                            }
                        })
                        .onError( defaultErrorHandler() );
            });
        });
    }


    public EntityRepository repo() {
        return repo;
    }


    public UnitOfWork unitOfWork() {
        return uow;
    }


    public Promise<UnitOfWork> settings() {
        return new WaitFor<UnitOfWork>( () -> settingsUow != null ).thenSupply( () -> settingsUow ).start();
    }

    public Promise<UnitOfWork> modifiableSettings() {
        return new WaitFor<UnitOfWork>( () -> settingsRepo != null ).thenSupply( () -> settingsRepo.newUnitOfWork() ).start();
    }

}
