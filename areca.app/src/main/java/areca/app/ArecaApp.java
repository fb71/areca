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
import java.util.Locale;

import org.teavm.jso.browser.Location;
import org.teavm.jso.browser.Navigator;
import org.polymap.model2.Entity;
import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.store.tidbstore.IDBStore;

import areca.app.model.Anchor;
import areca.app.model.CarddavSettings;
import areca.app.model.Contact;
import areca.app.model.ImapSettings;
import areca.app.model.MatrixSettings;
import areca.app.model.Message;
import areca.app.model.SmtpSettings;
import areca.app.service.Service;
import areca.app.service.SyncableService;
import areca.app.ui.GeneralErrorPage;
import areca.app.ui.StartPage;
import areca.common.Assert;
import areca.common.Platform;
import areca.common.ProgressMonitor;
import areca.common.Promise;
import areca.common.Timer;
import areca.common.WaitFor;
import areca.common.base.Consumer.RConsumer;
import areca.common.event.EventManager;
import areca.common.event.IdleAsyncEventManager;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.rt.teavm.SimpleBrowserHistoryStrategy;
import areca.rt.teavm.TeaApp;
import areca.rt.teavm.ui.UIComponentRenderer;
import areca.ui.Size;
import areca.ui.component2.Progress;
import areca.ui.component2.Text;
import areca.ui.component2.UIComposite;
import areca.ui.layout.MaxWidthLayout;
import areca.ui.layout.RowConstraints;
import areca.ui.layout.RowLayout;
import areca.ui.pageflow.Pageflow;

/**
 *
 * @author Falko Bräutigam
 */
public class ArecaApp extends TeaApp {

    static final Log LOG = LogFactory.getLog( ArecaApp.class );

    public static ArecaApp instance() {
        return instance != null ? (ArecaApp)instance : (ArecaApp)(instance = new ArecaApp());
    }

    public static ArecaApp current() {
        return instance();
    }

    public static String proxiedUrl( String url ) {
        return "http?uri=" + url;
    }

    /**
     * XXX {@link Platform}!!
     */
    public static Locale locale() {
        try {
            var splitted = Navigator.getLanguage().split( "-" );
            LOG.info( "Lang: %s", Arrays.asList( splitted ) );
            return splitted.length == 1
                    ? new Locale( splitted[0] )
                    : new Locale( splitted[0], splitted[1] );
        }
        catch (Exception e) {
            return Locale.getDefault();
        }
    }

    /**
     * XXX {@link Platform}!!
     */
    public static String hostname() {
        return Location.current().getHostName();
    }

    static {
        EventManager.setInstance( new IdleAsyncEventManager() );
    }

    public static final List<ClassInfo<? extends Entity>> APP_ENTITY_TYPES = asList(
            Message.info, Contact.info, Anchor.info );

    public static final List<ClassInfo<? extends Entity>> SETTINGS_ENTITY_TYPES = asList(
            ImapSettings.info, MatrixSettings.info, SmtpSettings.info, CarddavSettings.info );


    // instance *******************************************

    protected EntityRepository      repo;

    protected UnitOfWork            uow;

    protected EntityRepository      settingsRepo;

    /** {@link Services API} to work with {@link Service}s. */
    public Services                 services = new Services( this );

    /** {@link Synchronization API} for {@link SyncableService}s. */
    public Synchronization          synchronization = new Synchronization( this );

    /** {@link ModelUpdates API} for model update operations. */
    public ModelUpdates             modelUpdates = new ModelUpdates( this );

    private UIComposite             mainBody;

    private UIComposite             progressBody;

    private boolean                 debug;


    protected ArecaApp() {
        EntityRepository.newConfiguration()
                .entities.set( APP_ENTITY_TYPES )
                .store.set( new IDBStore( "areca.app", 28, true ) )
                .create()
                .onSuccess( result -> {
                    repo = result;
                    uow = repo.newUnitOfWork();
                    LOG.info( "Database and model repo initialized." );
                });

        EntityRepository.newConfiguration()
                .entities.set( SETTINGS_ENTITY_TYPES )
                .store.set( new IDBStore( "areca.app.settings", 5, true ) )
                .create()
                .onSuccess( result -> {
                    settingsRepo = result;
                    //settingsUow = settingsRepo.newUnitOfWork();
                    LOG.info( "Settings database and model repo initialized." );
                });
    }


    public boolean debug() {
        return debug;
    }


    public void createUI( @SuppressWarnings("hiding") boolean debug ) {
        this.debug = debug;
        Promise.setDefaultErrorHandler( defaultErrorHandler() );

        UIComponentRenderer.start();

        super.createUI( rootWindow -> {
            //VisualActionFeedback.start();

            rootWindow.layout.set( MaxWidthLayout.width( 700 ) );
            rootWindow.add( new UIComposite() {{
                layout.set( new RowLayout() {{orientation.set( VERTICAL ); fillWidth.set( true );}} );
                mainBody = add( new UIComposite() {{
                    layoutConstraints.set( new RowConstraints() {{height.set( rootWindow.size.value().height() - 5 );}} );
                }});

                // monitorBody
                progressBody = add( new UIComposite() {{
                    layoutConstraints.set( new RowConstraints() {{height.set( 5 );}} );
                    cssClasses.add( "ProgressContainer" );
                    layout.set( new RowLayout() {{margins.set( Size.of( 0, 0 ) ); spacing.set( 10 ); fillWidth.set( true ); fillHeight.set( true );}} );
                }});
            }});
            rootWindow.layout();

            Pageflow.start( mainBody ).create( new StartPage() ).open();

            SimpleBrowserHistoryStrategy.start( Pageflow.current() );
        });
    }


    public RConsumer<Throwable> defaultErrorHandler() {
        return (Throwable e) -> {
            if (e instanceof ProgressMonitor.CancelledException) {
                LOG.info( "Operation cancelled." );
            }
            else if (e instanceof Promise.CancelledException) {
                LOG.info( "Operation cancelled." );
            }
            else if (debug) {
                // get a meaningfull stracktrace in TeaVM
                throw (RuntimeException)e;
            }
            else {
                Pageflow.current().create( new GeneralErrorPage( e ) ).open();
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
            public ProgressMonitor beginTask( String name, int totalWork ) {
                Assert.that( taskName.isEmpty() );
                taskName = name;
                progressText.content.set( name );
                progressBody.layout();
                setTotalWork( totalWork );
                LOG.info( "Task: '%s' started", name );
                return this;
            }

            @Override
            public void setTotalWork( int workTotal ) {
                Assert.that( !taskName.isEmpty() );
                this.workTotal = workTotal;
                progress.max.set( (float)(workTotal) );
                LOG.debug( "PROGRESS: update: %s", workTotal );
            }

            @Override
            public ProgressMonitor subTask( String name ) {
                // subTaskName = name;
                return this;
            }

            @Override
            public ProgressMonitor worked( int work ) {
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
                return this;
            }

            @Override
            public ProgressMonitor done() {
                progress.value.set( Float.valueOf( workTotal ) );
                progressText.content.set( taskName + " 100%" );
                Platform.schedule( 500, () -> {
                    progress.dispose();
                    progressText.dispose();
                    progressBody.layout();
                });
                return this;
            }
        };
    }


    public EntityRepository repo() {
        return repo;
    }


    public UnitOfWork unitOfWork() {
        return uow;
    }


//    public Promise<UnitOfWork> settings() {
//        return new WaitFor<UnitOfWork>( () -> settingsUow != null ).thenSupply( () -> settingsUow ).start();
//    }

    public Promise<UnitOfWork> modifiableSettings() {
        return new WaitFor<UnitOfWork>( () -> settingsRepo != null ).thenSupply( () -> settingsRepo.newUnitOfWork() ).start();
    }

}
