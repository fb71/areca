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
import areca.app.model.ImapSettings;
import areca.app.service.imap.FolderListCommand;
import areca.app.service.imap.ImapRequest;
import areca.app.service.imap.ImapRequest.LoginCommand;
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
import areca.ui.viewer.Number2StringTransformer;
import areca.ui.viewer.TextViewer;

/**
 *
 * @author Falko Bräutigam
 */
public class ImapSettingsPage
        extends Page {

    private static final Log LOG = LogFactory.getLog( ImapSettingsPage.class );

    private PageContainer ui;

    private Promise<UnitOfWork> uow;


    @Override
    protected UIComponent doInit( UIComposite parent ) {
        this.ui = new PageContainer( this, parent );
        ui.title.set( "Email/IMAP Settings" );
        ui.body.layout.set( new RowLayout().orientation.set( VERTICAL ).fillWidth.set( true ).spacing.set( 15 ).margins.set( Size.of( 10, 10 ) ) );

        uow = ArecaApp.instance().settings();

        site.actions.add( new Action() {{
            icon.set( "add_circle_outline" );
            description.set( "Create new..." );
            handler.set( (UIEvent ev) -> {
                uow.waitForResult().get().createEntity( ImapSettings.class, proto -> {
                    proto.host.set( "mail.polymap.de" );
                    proto.port.set( 993 );
                    proto.username.set( String.format( "%s%s%s", "falko", "@", "polymap.de" ) );
                    proto.pwd.set( "..." );
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

        uow.waitForResult().get().query( ImapSettings.class ).executeCollect().onSuccess( list -> {
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
                    add( form.newField().label( "Host" )
                            .viewer( new TextViewer() )
                            .adapter( new PropertyAdapter<>( () -> settings.host ) )
                            .create() );
                    add( form.newField().label( "Port" )
                            .viewer( new TextViewer() )
                            .transformer( new Number2StringTransformer() )
                            .adapter( new PropertyAdapter<>( () -> settings.port ) )
                            .create() );
                    add( form.newField().label( "Username" )
                            .viewer( new TextViewer() )
                            .adapter( new PropertyAdapter<>( () -> settings.username ) )
                            .create() );
                    add( form.newField().label( "Password" )
                            .viewer( new TextViewer() )
                            .adapter( new PropertyAdapter<>( () -> settings.pwd ) )
                            .create() );

                    add( new UIComposite() {{
                        layoutConstraints.set( new RowConstraints().height.set( 40 ) );
                        layout.set( new RowLayout().fillWidth.set( true ).fillHeight.set( true ).spacing.set( 10 ) );
                        // check
                        add( new Button() {{
                            var badge = new Badge( this );
                            label.set( "CHECK" );
                            events.on( EventType.SELECT, ev -> {
                                badge.content.set( ".." );
                                newRequest( settings ).submit()
                                        .onSuccess( (s,command) -> {
                                            badge.content.set( ":)" );
                                        })
                                        .onError( e -> {
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

    protected ImapRequest newRequest( ImapSettings settings ) {
        LOG.info( "Settings: %s", settings.port.get() );
        LOG.info( "Settings: %s", settings.port.get() );
        var port = settings.port.get();
        LOG.info( "Settings: %s", port );
        return new ImapRequest( self -> {
            self.host = settings.host.get();
            self.port = 993; //port;
            self.loginCommand = new LoginCommand( settings.username.get(), settings.pwd.get() );
            self.commands.add( new FolderListCommand() );
            LOG.info( "Settings: %s -> %s", (int)settings.port.get(), self.port );
        });
    }

}
