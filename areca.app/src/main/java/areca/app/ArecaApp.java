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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.teavm.jso.browser.Location;
import org.teavm.jso.browser.Navigator;
import org.teavm.jso.browser.Window;

import org.polymap.model2.Entity;
import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.store.tidbstore.IDBStore;

import areca.app.model.Address;
import areca.app.model.Anchor;
import areca.app.model.CarddavSettings;
import areca.app.model.Contact;
import areca.app.model.ImapSettings;
import areca.app.model.MatrixSettings;
import areca.app.model.Message;
import areca.app.model.SmtpSettings;
import areca.app.service.Service;
import areca.app.service.TransportService;
import areca.app.service.TransportService.Transport;
import areca.app.service.TransportService.TransportContext;
import areca.app.service.carddav.CarddavService;
import areca.app.service.mail.MailService;
import areca.app.service.matrix.MatrixService;
import areca.app.ui.StartPage;
import areca.common.Assert;
import areca.common.Platform;
import areca.common.ProgressMonitor;
import areca.common.Promise;
import areca.common.Timer;
import areca.common.WaitFor;
import areca.common.base.Consumer.RConsumer;
import areca.common.base.Function.RFunction;
import areca.common.base.Opt;
import areca.common.base.Sequence;
import areca.common.event.AsyncEventManager;
import areca.common.event.EventManager;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
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
        EventManager.setInstance( new AsyncEventManager() );
    }

    public static final List<ClassInfo<? extends Entity>> APP_ENTITY_TYPES = asList(
            Message.info, Contact.info, Anchor.info );

    public static final List<ClassInfo<? extends Entity>> SETTINGS_ENTITY_TYPES = asList(
            ImapSettings.info, MatrixSettings.info, SmtpSettings.info, CarddavSettings.info );


    // instance *******************************************

    private List<? extends Service> services = Arrays.asList(
            new CarddavService(),
            new MailService(),
            new MatrixService());

    protected EntityRepository      repo;

    protected UnitOfWork            uow;

    protected EntityRepository      settingsRepo;

    protected UnitOfWork            settingsUow;

    private UIComposite             mainBody;

    private UIComposite             progressBody;

    private ModelUpdates            modelUpdates;

    private Synchronization         synchronization;


    protected ArecaApp() {
        modelUpdates = new ModelUpdates( this );
        synchronization = new Synchronization( this );

        EntityRepository.newConfiguration()
                .entities.set( APP_ENTITY_TYPES )
                .store.set( new IDBStore( "areca.app", 27, true ) )
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
                layoutConstraints.set( new RowConstraints() {{height.set( rootWindow.size.value().height() - 5 );}} );
            }});

            // monitorBody
            progressBody = rootWindow.add( new UIComposite() {{
                layoutConstraints.set( new RowConstraints() {{height.set( 5 );}} );
                cssClasses.add( "ProgressContainer" );
                layout.set( new RowLayout() {{margins.set( Size.of( 0, 0 ) ); spacing.set( 10 ); fillWidth.set( true ); fillHeight.set( true );}} );
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


    @SuppressWarnings("unchecked")
    public <R> Sequence<R,RuntimeException> services( Class<R> type ) {
        return Sequence.of( services ).filter( type::isInstance ).map( s -> (R)s );
    }


    public void forceIncrementalSync( int delay ) {
        synchronization.forceIncremental( delay );
    }


    /**
     * One Transport that can be used for the given receipient, or {@link Promise#absent()}
     * if there is no Transport that can handle the receipient.
     *
     * @return One or {@link Promise#absent()}
     */
    public Promise<Opt<Transport>> transportFor( Address receipient ) {
        var ctx = new TransportContext() {
            @Override public ProgressMonitor newMonitor() {
                return ArecaApp.instance().newAsyncOperation();
            }
        };

        LOG.info( "Transports for: %s ", receipient );
        var s = services( TransportService.class ).toList();
        if (s.isEmpty()) {
            return Promise.absent();
        }
        return Promise
                .joined( s.size(), i -> s.get( i ).newTransport( receipient, ctx ) )
                .onSuccess( l -> LOG.info( "Transports: %s", l ) )
                .reduce( new ArrayList<Transport>(), (r,transports) -> r.addAll( transports ) )
                .map( l -> l.isEmpty() ? Opt.absent() : Opt.of( l.get( 0 ) ) );
    }


    /**
     * Schedule the given model update operation.
     *
     * @see ModelUpdates
     */
    public void scheduleModelUpdate( RFunction<UnitOfWork,Promise<?>> update ) {
        modelUpdates.schedule( update );
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
