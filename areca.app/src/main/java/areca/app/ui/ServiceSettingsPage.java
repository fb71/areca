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
package areca.app.ui;

import static areca.ui.Orientation.VERTICAL;

import org.apache.commons.lang3.mutable.MutableObject;

import org.polymap.model2.Entity;
import org.polymap.model2.runtime.UnitOfWork;

import areca.app.ArecaApp;
import areca.common.Assert;
import areca.common.Platform;
import areca.common.Promise;
import areca.common.Scheduler.Priority;
import areca.common.Timer;
import areca.common.base.Supplier.RSupplier;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Size;
import areca.ui.component2.Badge;
import areca.ui.component2.Button;
import areca.ui.component2.Events.EventType;
import areca.ui.component2.Property;
import areca.ui.component2.Property.ReadWrite;
import areca.ui.component2.Text;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.layout.RowConstraints;
import areca.ui.layout.RowLayout;
import areca.ui.pageflow.Page;
import areca.ui.pageflow.PageContainer;
import areca.ui.viewer.form.Form;

/**
 *
 * @param <S> The type of the settings {@link Entity}.
 * @author Falko Bräutigam
 */
public abstract class ServiceSettingsPage<S extends Entity>
        extends Page {

    private static final Log LOG = LogFactory.getLog( ServiceSettingsPage.class );

    public enum Status {
        UNKNOWN, VALID, INVALID
    }

    protected PageContainer       ui;

    protected Promise<UnitOfWork> uow;

    protected Class<S>            settingsType;


    public ServiceSettingsPage( Class<S> settingsType ) {
        this.settingsType = settingsType;
    }

    protected abstract S newSettings();

    protected abstract UIComposite buildCheckingForm( RSupplier<S> settings, Form form );


    @Override
    protected UIComponent onCreateUI( UIComposite parent ) {
        this.ui = new PageContainer( this, parent );
        ui.body.layout.set( new RowLayout().orientation.set( VERTICAL ).fillWidth.set( true ).spacing.set( 15 ).margins.set( Size.of( 10, 10 ) ) );

        uow = ArecaApp.instance().modifiableSettings();

//        site.actions.add( new Action() {{
//            icon.set( "add_circle_outline" );
//            description.set( "Create new..." );
//            handler.set( (UIEvent ev) -> {
//                newSettings();
//                createUI( ui.body );
//            });
//        }});
        loadDataAndCreateForm( ui.body );
        return ui;
    }


    protected void loadDataAndCreateForm( UIComposite container ) {
        var t = Timer.start();
        container.components.disposeAll();

        // (pre)build (not load) the form for the first Entity
        Form form1 = new Form();
        MutableObject<S> settings1 = new MutableObject<>();
        container.add( buildCheckingForm( () -> settings1.getValue(), form1 ) );
        container.layout();

        LOG.info( "Form created (%s)", t.elapsedHumanReadable() );
        t.restart();

        uow.waitForResult().get().query( settingsType )
                .executeCollect()
                //.then( list -> Platform.schedule( 1000, () -> list ) )
                .priority( Priority.BACKGROUND )
                .onSuccess( list -> {
                    LOG.info( "Query done (%s)", t.elapsedHumanReadable() );
                    t.restart();

                    if (list.isEmpty()) {
                        container.components.disposeAll();
                        container.add( new Text().content.set( "No settings yet." ) );
                        container.add( new Button() {{
                            label.set( "CREATE" );
                            events.on( EventType.SELECT, ev -> {
                                newSettings();
                                loadDataAndCreateForm( ui.body );
                            });
                        }});
                    }
                    else {
                        settings1.setValue( list.get( 0 ) );
                        form1.revert();
                        LOG.info( "Form loaded (%s)", t.elapsedHumanReadable() );

                        Assert.that( list.size() <= 1, "Multi forms are not supported yet" );
                    }
                    container.layout();
                });
    }

    /**
     *
     */
    protected abstract class CheckingForm
            extends UIComposite {

        protected RSupplier<S> settings;

        protected Form  form;

        public ReadWrite<?,Status> status = Property.rw( this, "valid", Status.UNKNOWN );


        public CheckingForm( RSupplier<S> settings, Form form ) {
            this.settings = settings;
            this.form = form;
            layoutConstraints.set( new RowConstraints().height.set( 250 ) );
            layout.set( new RowLayout().orientation.set( VERTICAL ).fillWidth.set( true )
                    .spacing.set( 25 ).margins.set( Size.of( 10, 15 ) ) );
            //bordered.set( true );

            buildForm();

            add( new UIComposite() {{
                layoutConstraints.set( new RowConstraints().height.set( 40 ) );
                layout.set( new RowLayout().fillWidth.set( true ).fillHeight.set( true ).spacing.set( 10 ) );
                buildButtons( this );
            }});
        }


        protected abstract void buildForm();

        protected abstract void buildButtons( UIComposite container );


        protected Button createCheckButton( RSupplier<Promise<?>> checker ) {
            return new Button() {{
                var badge = addDecorator( new Badge() ).get();
                label.set( "CHECK" );
                events.on( EventType.SELECT, ev -> {
                    badge.content.set( "..." );
                    label.set( "Checking ..." );
                    enabled.set( false );
                    form.submit();
                    checker.supply()
                            .onSuccess( ( s, command ) -> {
                                label.set( "CHECK" );
                                enabled.set( true );
                                badge.content.set( ":)" );
                                status.set( Status.VALID );
                            })
                            .onError( e -> {
                                label.set( "CHECK" );
                                enabled.set( true );
                                badge.content.set( ":(" );
                                status.set( Status.INVALID );
                                pageSite.createPage( new GeneralErrorPage( e ) ).origin( position.get() ).open();
                            });
                            //.onError( ArecaApp.instance().defaultErrorHandler() );
                });
            }};
        }

        protected Button createSubmitButton() {
            return new Button() {{
                var badge = addDecorator( new Badge() ).get();
                label.set( "SUBMIT" );
                status.onInitAndChange( (newStatus, __) -> {
                    enabled.set( status.value() == Status.VALID );
                });
                events.on( EventType.SELECT, ev -> {
                    form.submit();
                    uow.waitForResult().get().submit().onSuccess( submitted -> {
                        LOG.info( "Submitted: %s", submitted );
                        badge.content.set( "OK" );
                        Platform.schedule( 750, () -> pageSite.close() );
                    });
                });
            }};
        }
    }
}
