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
import static org.apache.commons.lang3.StringUtils.abbreviate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;

import java.text.DateFormat;

import org.apache.commons.lang3.StringUtils;

import org.polymap.model2.ManyAssociation;
import org.polymap.model2.runtime.Lifecycle.State;

import areca.app.ArecaApp;
import areca.app.model.Message;
import areca.app.service.TransportService;
import areca.app.service.TransportService.Sent;
import areca.app.service.TransportService.Transport;
import areca.app.service.TransportService.TransportContext;
import areca.app.service.TypingEvent;
import areca.common.Assert;
import areca.common.Platform;
import areca.common.ProgressMonitor;
import areca.common.Promise;
import areca.common.Timer;
import areca.common.base.Opt;
import areca.common.event.EventManager;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Align.Vertical;
import areca.ui.Position;
import areca.ui.Size;
import areca.ui.component2.Badge;
import areca.ui.component2.Button;
import areca.ui.component2.Events.EventType;
import areca.ui.component2.ScrollableComposite;
import areca.ui.component2.Text;
import areca.ui.component2.TextField;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.layout.LayoutManager;
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

    public static final DateFormat  sdf = DateFormat.getDateTimeInstance( DateFormat.DEFAULT, DateFormat.DEFAULT );

    public static final int         MESSAGE_MARK_READ_DELAY = 5000;

    protected ManyAssociation<Message>   src;

    protected PageContainer         ui;

    protected long                  timeout = 280;  // 300ms timeout before page animation starts

    protected ScrollableComposite   messagesContainer;

    protected UIComposite           inputContainer;

    protected MessageCard           selectedCard;

    protected TextField             messageTextInput;


    protected MessagesPage( ManyAssociation<Message> src ) {
        this.src = src;
    }


    @Override
    protected UIComponent doInit( UIComposite parent ) {
        ui = new PageContainer( this, parent );
        ui.title.set( "Messages" );

        ui.body.layout.set( new RowLayout().orientation.set( VERTICAL )
                .fillHeight.set( true )
                .fillWidth.set( true ) );

        // input field
        inputContainer = ui.body.add( new UIComposite() {{
            layoutConstraints.set( new RowConstraints().height.set( 55 ) );
            layout.set( new RowLayout().margins.set( Size.of( 8, 8 ) ).spacing.set( 8 ).fillHeight.set(  true ).fillWidth.set( true ) );
            messageTextInput = add( new TextField() {{
                Platform.schedule( 2000, () -> focus.set( true ) );

            }});
            add( new Button() {{
                layoutConstraints.set( new RowConstraints().width.set( 70 ) );
                icon.set( "send" );
                tooltip.set( "Do it! :)" );
                events.on(  EventType.SELECT, ev -> {
                    send();
                });
            }});
        }});

        // typing...
        ui.body.add( new Text() {{
            layoutConstraints.set( new RowConstraints().height.set( 10 ) );
            cssClasses.add( "TypingText" );
            EventManager.instance()
                    .subscribe( (TypingEvent ev) -> {
                        if (ev.typing) {
                            content.set( String.format( "%s ...", ev.getSource() ) );
                        } else {
                            content.set( "" );
                        }
                    })
                    .performIf( ev -> ev instanceof TypingEvent )
                    .unsubscribeIf( () -> isDisposed() );
        }});

        // messages
        messagesContainer = ui.body.add( new ScrollableComposite() {{
            layout.set( new RowLayout() {{
                componentOrder.set( Comparator.<MessageCard>naturalOrder().reversed() );
                orientation.set( VERTICAL );
                fillWidth.set( true );
                spacing.set( 10 );
                margins.set( Size.of( 8, 1 ) );
            }});
            add( new Text().content.set( "Loading..." ) );
        }});

        fetchMessages();
        return ui;
    }


    protected void send() {
        Assert.notNull( selectedCard, "selectedCard == null" );
        var ctx = new TransportContext() {
            @Override public ProgressMonitor newMonitor() {
                return ArecaApp.instance().newAsyncOperation();
            }
        };
        var receipients = selectedCard.message.from.get();
        var services = ArecaApp.instance().services( TransportService.class ).toList();
        Promise.joined( services.size(), i -> services.get( i ).newTransport( receipients, ctx ) )
                .reduce2( (Transport)null, (r, transport) -> transport != null ? transport : r )
                .then( transport -> {
                    if (transport == null) {
                        LOG.info( "No transport found for: %s", receipients );
                        // XXX UI
                        return Promise.<Sent>absent();
                    }
                    else {
                        LOG.info( "Transport found for: %s - %s", receipients, transport );
                        return transport.send( messageTextInput.content.value() ).map( result -> Opt.of( result ) );
                    }
                })
                .onSuccess( result -> {
                    result.ifPresent( sent -> {
                        LOG.info( "Transport sent! :) - %s", sent );
                    });
                })
                .onError( ArecaApp.instance().defaultErrorHandler() );

    }


    protected void fetchMessages() {
        // XXX wait for repo to show up
        if (ArecaApp.instance().repo() == null) {
            LOG.info( "waiting for repo..." );
            Platform.schedule( 100, () -> fetchMessages() );
            return;
        }
        messagesContainer.components.disposeAll();
        var timer = Timer.start();
        var chunk = new ArrayList<MessageCard>();

        src.fetch().onSuccess( (ctx,opt) -> {
            opt.ifPresent( msg -> chunk.add( new MessageCard( msg ) ) );

            // deferred layout chunk
            if (timer.elapsed( MILLISECONDS ) > timeout || ctx.isComplete()) {
                LOG.info( "" + timer.elapsedHumanReadable() );
                timer.restart();
                timeout = 1000;

                chunk.forEach( card -> messagesContainer.add( card ) );
                messagesContainer.layout();

                var last = chunk.get( chunk.size()-1 );
                chunk.clear();
                Platform.schedule( 1000, () -> {
                    last.scrollIntoView.set( Vertical.BOTTOM );
                    last.select( true );
                });
            }
        });
    }


    /**
     *
     */
    public class MessageCard
            extends UIComposite
            implements Comparable<MessageCard> {

        private static final String CSS = "MessageCard";
        private static final String CSS_SELECTED = "MessageCard-selected";
        private static final int    MARGINS = 10;
        private static final int    HEADER = 12;
        private static final int    SPACING = 5;
        private static final int    MAX_LINES = 5;

        public Message          message;

        protected boolean       isSelected;

        protected Text          contentText;


        public MessageCard( Message msg ) {
            this.message = msg;
            cssClasses.add( CSS );

            events.on( EventType.SELECT, ev -> {
                toggle();
            });

            var fromText = add( new Text() {{
                content.set( abbreviate( msg.from.get(), 30 ) );
                cssClasses.add( "FromText" );
            }});
            var dateText = add( new Text() {{
                content.set( df.format( new Date( msg.date.get()) ) );
                cssClasses.add( "DateText" );
            }});
            contentText = add( new Text() {{
                content.set( StringUtils.abbreviate( msg.content.get(), 250 ) );
            }});

            // layout
            layout.set( new LayoutManager() {
                @Override public void layout( UIComposite composite ) {
                    var s = clientSize.value().substract( MARGINS, MARGINS );
                    fromText.position.set( Position.of( MARGINS, MARGINS-3) );
                    fromText.size.set( Size.of( s.width()/2, HEADER ) );

                    dateText.position.set( Position.of( s.width()/2, MARGINS-3) );
                    dateText.size.set( Size.of( s.width()/2, HEADER ) );

                    contentText.position.set( Position.of( MARGINS, MARGINS + HEADER + SPACING) );
                    contentText.size.set( Size.of( s.width(), s.height() - HEADER - SPACING ) );
                }
            });

            if (message.unread.get()) {
                new Badge( this ) {{
                    content.set( "X" );
                    // listen to updates
                    message.onLifecycle( State.AFTER_SUBMIT, ev -> {
                        content.set( message.unread.get() ? "X" : null );
                    })
                    .unsubscribeIf( () -> isDisposed() );
                }};
            }
        }


        @Override
        public int computeMinHeight( int width ) {
            var charsPerLine = (int)((float)width / 8f);
            var lines = (message.content.get().length() / charsPerLine) + 1;
            LOG.info( "WIDHT: %s, charsPerLine: %s, lines: %s", width, charsPerLine, lines );
            lines = Math.min( MAX_LINES, lines );

            contentText.content.set( abbreviate( message.content.get(), lines * charsPerLine ) );

            return (MARGINS*2) + HEADER + SPACING + (lines * 16);
        }


        public void select( boolean select ) {
            //LOG.info( "Selected: %s, %s -> %s", message.content.get(), isSelected, select );
            // select
            if (!isSelected && select) {
                if (selectedCard != null) {
                    selectedCard.select( false );
                }
                cssClasses.add( CSS_SELECTED );
                selectedCard = this;
                delayedMarkRead();
            }
            // unselect
            else if (isSelected && !select) {
                cssClasses.remove( CSS_SELECTED );
                selectedCard = null;
            }
            isSelected = select;
        }


        public void toggle() {
            select( !isSelected );
        }

        protected void delayedMarkRead() {
            if (message.unread.get()) {
                Platform.schedule( MESSAGE_MARK_READ_DELAY, () -> {
                    if (selectedCard == this && message.unread.get()) {
                        // FIXME fast but AnchorsCloudPage gets no event
                        message.unread.set( false );
                        message.context.getUnitOfWork().submit();
                    }
                });
            }
        }

        @Override
        public int compareTo( MessageCard other ) {
            return message.date.get().compareTo( other.message.date.get() );
        }
    }

}
