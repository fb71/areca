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

import org.polymap.model2.runtime.UnitOfWork;

import areca.app.ArecaApp;
import areca.app.model.MatrixSettings;
import areca.app.service.matrix.MatrixClient;
import areca.common.Promise;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Action;
import areca.ui.Size;
import areca.ui.component2.Badge;
import areca.ui.component2.Button;
import areca.ui.component2.Events.EventType;
import areca.ui.component2.Events.UIEvent;
import areca.ui.component2.Text;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.form.Form;
import areca.ui.layout.RowConstraints;
import areca.ui.layout.RowLayout;
import areca.ui.pageflow.Page;
import areca.ui.pageflow.PageContainer;
import areca.ui.viewer.TextFieldViewer;

/**
 *
 * @author Falko Bräutigam
 */
public class MatrixSettingsPage
        extends Page {

    private static final Log LOG = LogFactory.getLog( MatrixSettingsPage.class );

    private PageContainer ui;

    private Promise<UnitOfWork> uow;


    @Override
    protected UIComponent doInit( UIComposite parent ) {
        this.ui = new PageContainer( this, parent );
        ui.title.set( "Matrix Settings" );
        ui.body.layout.set( new RowLayout().orientation.set( VERTICAL ).fillWidth.set( true ).spacing.set( 15 ).margins.set( Size.of( 10, 10 ) ) );

        uow = ArecaApp.instance().settings();

        site.actions.add( new Action() {{
            icon.set( "add_circle_outline" );
            description.set( "Create new..." );
            handler.set( (UIEvent ev) -> {
                uow.waitForResult().get().createEntity( MatrixSettings.class, proto -> {
                    proto.baseUrl.set( "https://matrix.fulda.social" );
                    proto.username.set( "@bolo:fulda.social" );
                    proto.accessToken.set( "..." );
                });
                fetchEntities( ui.body );
            });
        }});
        fetchEntities( ui.body );
        return ui;
    }


    protected void fetchEntities( UIComposite container) {
        container.components.disposeAll();
        container.add( new Text().content.set( "Loading..." ) );
        container.layout();

        uow.waitForResult().get().query( MatrixSettings.class ).executeCollect().onSuccess( list -> {
            container.components.disposeAll();
            if (list.isEmpty()) {
                container.add( new Text().content.set( "No settings yet." ) );
            }
            for (var settings : list) {
                container.add( new UIComposite() {{
                    layoutConstraints.set( new RowConstraints().height.set( 250 ) );
                    layout.set( new RowLayout().orientation.set( VERTICAL ).fillWidth.set( true ).spacing.set( 10 ).margins.set( Size.of( 10, 10 ) ) );
                    bordered.set( true );
                    var form = new Form();
                    add( form.newField().label( "Base URL" )
                            .viewer( new TextFieldViewer() )
                            .adapter( new PropertyAdapter<>( () -> settings.baseUrl ) )
                            .create() );
                    add( form.newField().label( "Username" )
                            .viewer( new TextFieldViewer() )
                            .adapter( new PropertyAdapter<>( () -> settings.username ) )
                            .create() );
                    add( form.newField().label( "Password" )
                            .viewer( new TextFieldViewer() )
                            .adapter( new PropertyAdapter<>( () -> settings.pwd ) )
                            .create() );
                    add( form.newField().label( "Access token" )
                            .viewer( new TextFieldViewer() )
                            .adapter( new PropertyAdapter<>( () -> settings.accessToken ) )
                            .create() );

                    add( new UIComposite() {{
                        layoutConstraints.set( new RowConstraints().height.set( 40 ) );
                        layout.set( new RowLayout().fillWidth.set( true ).fillHeight.set( true ).spacing.set( 10 ) );
                        // check
                        add( new Button() {{
                            var badge = new Badge( this );
                            label.set( "CHECK" );
                            events.on( EventType.SELECT, ev -> {
                                badge.content.set( "..." );
                                form.submit();
                                var matrix = MatrixClient.create( ArecaApp.proxiedUrl( settings.baseUrl.get() ),
                                        settings.accessToken.get(), settings.username.get() );
                                matrix.startClient();
                                matrix.waitForStartup()
                                        .onSuccess( ( s, command ) -> {
                                            matrix.stopClient();
                                            badge.content.set( ":)" );
                                        })
                                        .onError( e -> {
                                            matrix.stopClient();
                                            badge.content.set( ":(" );
                                        });
                            });
                        }});
                        // submit
                        add( new Button() {{
                            var badge = new Badge( this );
                            label.set( "SUBMIT" );
                            events.on( EventType.SELECT, ev -> {
                                form.submit();
                                uow.waitForResult().get().submit().onSuccess( submitted -> {
                                    LOG.info( "Submitted: %s", submitted );
                                    badge.content.set( "OK" );
                                });
                            });
                        }});
                    }});
                }});
            }
            container.layout();
        });
    }

}
