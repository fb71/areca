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
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.store.tidbstore.IDBStore;

import areca.app.model.Anchor;
import areca.app.model.Contact;
import areca.app.model.ImapSettings;
import areca.app.model.Message;
import areca.app.service.Service;
import areca.app.service.SyncableService;
import areca.app.service.SyncableService.SyncContext;
import areca.app.service.carddav.CarddavService;
import areca.app.service.imap.ImapService;
import areca.app.ui.StartPage;
import areca.common.Assert;
import areca.common.Platform;
import areca.common.ProgressMonitor;
import areca.common.Promise;
import areca.common.Timer;
import areca.common.base.Consumer.RConsumer;
import areca.common.base.Opt;
import areca.common.base.Sequence;
import areca.common.base.Supplier.RSupplier;
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

    private EntityRepository        settingsRepo;

    private UnitOfWork              settingsUow;

    private UIComposite             mainBody;

    private UIComposite             progressBody;


    protected ArecaApp() {
        EntityRepository.newConfiguration()
                .entities.set( asList( Message.info, Contact.info, Anchor.info) )
                .store.set( new IDBStore( "areca.app", 6, true ) )
                .create()
                .onSuccess( result -> {
                    repo = result;
                    uow = repo.newUnitOfWork();
                    LOG.info( "Database and model repo initialized." );
                });

        EntityRepository.newConfiguration()
                .entities.set( asList( ImapSettings.info ) ).store
                .set( new IDBStore( "areca.app.settings", 1, true ) )
                .create()
                .onSuccess( result -> {
                    settingsRepo = result;
                    settingsUow = settingsRepo.newUnitOfWork();
                    LOG.info( "Settings database and model repo initialized." );
                });
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
                layoutConstraints.set( new RowConstraints() {{height.set( rootWindow.size.value().height() - 10 );}} );
            }});

            // monitorBody
            progressBody = rootWindow.add( new UIComposite() {{
                layoutConstraints.set( new RowConstraints() {{height.set( 10 );}} );
                cssClasses.add( "ProgressContainer" );
                layout.set( new RowLayout() {{margins.set( Size.of( 3, 3 ) ); spacing.set( 10 ); fillWidth.set( true ); fillHeight.set( true );}} );

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
                Assert.that( !taskName.isEmpty() );
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
        services( SyncableService.class ).forEach( (service,i) -> {
            Platform.schedule( 3000 * i, () -> { // XXX start imap after contacts are there
                var ctx = new SyncContext() {{
                    monitor = newAsyncOperation();
                    uowFactory = () -> repo().newUnitOfWork();
                }};
                service.newSync( ctx ).start();
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
        return new WaitForCondition<>( () -> settingsUow != null, () -> settingsUow );
    }


    /**
     * Acts more like a lazily init variable that caches its result (in settingsUow).
     */
    public static class WaitForCondition<U>
            extends Promise.Completable<U> {

        protected RSupplier<Boolean>    condition;

        protected RSupplier<U>          factory;

        public WaitForCondition( RSupplier<Boolean> condition, RSupplier<U> factory ) {
            this.condition = condition;
            this.factory = factory;
            waitForCondition();
        }

        protected void waitForCondition() {
            if (!condition.get()) {
                LOG.info( "WAITING: ..." );
                Platform.schedule( 100, () -> waitForCondition() );
            }
            else {
                LOG.info( "WAITING: done." );
                var value = factory.supply();

                // if the condition matches in the first run,
                // then give the caller time to register its onSuccess handlers
                Platform.async( () -> complete( value ) );

                // XXX Hack: for ImapSettingsPage
                waitForResult = value;
            }
        }

        public Opt<U> waitForResult() {
            return Opt.of( waitForResult );
        }
    }
}
