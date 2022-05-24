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
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.ArrayList;
import org.apache.commons.lang3.StringUtils;

import org.polymap.model2.ManyAssociation;

import areca.app.ArecaApp;
import areca.app.model.Message;
import areca.common.Platform;
import areca.common.Timer;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Size;
import areca.ui.component2.Text;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.layout.RowConstraints;
import areca.ui.layout.RowLayout;
import areca.ui.pageflow.Page;
import areca.ui.pageflow.PageContainer;

/**
 *
 * @author Falko Bräutigam
 */
public class MessagesPage extends Page {

    private static final Log LOG = LogFactory.getLog( MessagesPage.class );

    protected ManyAssociation<Message>   src;

    protected PageContainer     ui;

    protected long              timeout = 280;  // 300ms timeout before page animation starts


    protected MessagesPage( ManyAssociation<Message> src ) {
        this.src = src;
    }


    @Override
    protected UIComponent doInit( UIComposite parent ) {
        ui = new PageContainer( this, parent );
        ui.title.set( "Messages" );

        ui.body.layout.set( new RowLayout() {{
            orientation.set( VERTICAL ); fillWidth.set( true ); spacing.set( 5 ); margins.set( Size.of( 5, 5 ) );}});
        ui.body.add( new Text().content.set( "Loading..." ) );
        fetchMessages();
        return ui;
    }


    protected void fetchMessages() {
        // XXX wait for repo to show up
        if (ArecaApp.instance().repo() == null) {
            LOG.info( "waiting for repo..." );
            Platform.schedule( 100, () -> fetchMessages() );
            return;
        }
        ui.body.components.disposeAll();
        var timer = Timer.start();
        var chunk = new ArrayList<UIComposite>();

        src.fetch().onSuccess( (ctx,msg) -> {
            if (msg != null) {
                chunk.add( createMessageCard( msg ) );
            }
            if (timer.elapsed( MILLISECONDS ) > timeout || ctx.isComplete()) {
                LOG.info( "" + timer.elapsedHumanReadable() );
                timer.restart();
                timeout = 1000;

                chunk.forEach( btn -> ui.body.add( btn ) );
                chunk.clear();
                ui.body.layout();
            }
        });
    }


    protected UIComposite createMessageCard( Message msg ) {
        return new UIComposite() {{
            cssClasses.add( "MessageCard" );
            layoutConstraints.set( new RowConstraints() {{height.set( 100 );}} );
            layout.set( new RowLayout() {{
                    orientation.set( VERTICAL ); fillWidth.set( true ); margins.set( Size.of( 10, 10 ) );}});
            add( new Text().content.set( StringUtils.abbreviate( msg.text.get(), 250 ) ) );
        }};
    }

}
