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

import java.util.ArrayList;
import areca.app.ArecaApp;
import areca.app.model.Anchor;
import areca.app.model.EntityLifecycleEvent;
import areca.common.Platform;
import areca.common.Timer;
import areca.common.event.EventCollector;
import areca.common.event.EventManager;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Size;
import areca.ui.component2.Badge;
import areca.ui.component2.Button;
import areca.ui.component2.Events.EventType;
import areca.ui.component2.Text;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.layout.RasterLayout;
import areca.ui.pageflow.Page;
import areca.ui.pageflow.PageContainer;

/**
 *
 * @author Falko Bräutigam
 */
public class AnchorsCloudPage
        extends Page {

    private static final Log LOG = LogFactory.getLog( AnchorsCloudPage.class );

    private UIComposite     body;

    private StartPage       page;


    /** Work from within StartPage */
    public AnchorsCloudPage( UIComposite body, StartPage page ) {
        this.site = page.site();
        this.page = page;
        this.body = body;
        createBody();
    }


    @Override
    protected UIComponent doInit( UIComposite parent ) {
        var ui = new PageContainer( this, parent );
        ui.title.set( "Anchors" );

        body = ui.body;
        createBody();
        return ui;
    }


    protected void createBody() {
        body.layout.set( new RasterLayout() {{
            spacing.set( 5 );
            margins.set( Size.of( 5, 5 ) );
            itemSize.set( Size.of( 74, 68 ) );
            componentOrder.set( (b1, b2) -> order( b1, b2 ) );
        }});
        body.add( new Text().content.set( "Loading..." ) );
        fetchAnchors();

        // listen to model updates
        var _100ms = new EventCollector<EntityLifecycleEvent>( 100 );
        EventManager.instance()
                .subscribe( (EntityLifecycleEvent ev) -> {
                    _100ms.collect( ev, collected -> {
                        fetchAnchors();
                    });
                })
                .performIf( EntityLifecycleEvent.class::isInstance )
                .unsubscribeIf( () -> body.isDisposed() );
    }


    protected int order( UIComponent c1, UIComponent c2 ) {
        var prio1 = c1.<String>optData( "prio" ).get();
        var prio2 = c2.<String>optData( "prio" ).get();
        return prio1.compareToIgnoreCase( prio2 );
    }


    protected long lastLayout;

    protected long timeout = 280;  // 300ms timeout before page animation starts

    protected void fetchAnchors() {
        // XXX wait for repo to show up
        if (ArecaApp.instance().repo() == null) {
            LOG.info( "waiting for repo..." );
            Platform.schedule( 100, () -> fetchAnchors() );
            return;
        }
        body.components.disposeAll();
        lastLayout = System.currentTimeMillis();
        var timer = Timer.start();
        var chunk = new ArrayList<Button>();

        ArecaApp.instance().unitOfWork()
                .query( Anchor.class )
                .execute()
                .onSuccess( (ctx,result) -> {
                    result.ifPresent( contact -> {
                        chunk.add( makeAnchorButton( contact ) );
                    });
                    var now = System.currentTimeMillis();
                    if (now > lastLayout + timeout || ctx.isComplete()) {
                        LOG.info( "" + timer.elapsedHumanReadable() );
                        timer.restart();
                        lastLayout = now;
                        timeout = 1000;

                        chunk.forEach( btn -> body.add( btn ) );
                        chunk.clear();
                        body.layout();
                    }
                });
    }


    protected Button makeAnchorButton( Anchor anchor ) {
        var btn = new Button();
        btn.cssClasses.add( "AnchorButton" );
//        btn.label.set( String.format( "%.7s %.7s",
//                contact.firstname.opt().orElse( "" ),
//                contact.lastname.opt().orElse( "" )) );
        btn.label.set( anchor.name.opt().orElse( "..." ) );
        btn.tooltip.set( anchor.name.opt().orElse( "" ) );

        btn.data( "prio", () -> anchor.name.get() );

        var badge = new Badge( btn );
        Platform.schedule( 2250, () -> {
            anchor.unreadMessagesCount().onSuccess( unread -> {
                //LOG.info( "Unread: " + unread );
                if (unread > 0) {
                    badge.content.set( String.valueOf( unread ) );
                }
            });
        });

        btn.events.on( EventType.SELECT, ev -> {
            site.put( anchor );
            site.pageflow().open( new MessagesPage( anchor.messages ), page, ev.clientPos() );
        });
        return btn;
    }


}
