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
import static org.apache.commons.lang3.StringUtils.abbreviate;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.text.DateFormat;

import org.polymap.model2.ManyAssociation;
import org.polymap.model2.query.Query.Order;
import org.polymap.model2.runtime.Lifecycle.State;

import areca.app.ArecaApp;
import areca.app.model.Address;
import areca.app.model.Message;
import areca.app.model.Message.ContentType;
import areca.app.model.ModelUpdateEvent;
import areca.app.service.MessageSentEvent;
import areca.app.service.TransportService.TransportMessage;
import areca.app.service.TypingEvent;
import areca.common.Assert;
import areca.common.Platform;
import areca.common.Promise;
import areca.common.Timer;
import areca.common.base.Opt;
import areca.common.base.Sequence;
import areca.common.event.EventManager;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Position;
import areca.ui.Size;
import areca.ui.component2.Badge;
import areca.ui.component2.Button;
import areca.ui.component2.Events.EventType;
import areca.ui.component2.Link;
import areca.ui.component2.Property;
import areca.ui.component2.Property.ReadWrite;
import areca.ui.component2.ScrollableComposite;
import areca.ui.component2.Text;
import areca.ui.component2.TextField;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.layout.DynamicLayoutManager;
import areca.ui.layout.DynamicLayoutManager.Component;
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

    public static final DateFormat  df = DateFormat.getDateTimeInstance( DateFormat.DEFAULT, DateFormat.DEFAULT, ArecaApp.locale() );

    public static final int         MESSAGE_MARK_READ_DELAY = 3000;

    protected ManyAssociation<Message>  src;

    protected ReadWrite<?,MessageCard>  selectedCard;  // create when UI is there in doInit()

    protected ReadWrite<?,String>       subject;

    protected String                title;

    protected PageContainer         ui;

    protected long                  timeout = 280;  // 300ms timeout before page animation starts

    protected ScrollableComposite   messagesContainer;

    protected UIComposite           inputContainer;

    protected UIComposite           headerContainer;

    protected TextField             messageTextInput;

    protected Map<Message,MessageCard> cards = new HashMap<>();


    protected MessagesPage( ManyAssociation<Message> src, String title ) {
        this.src = src;
        this.title = title;
    }


    @Override
    protected void doDispose() {
        cards.clear();
        ui.dispose();
    }


    @Override
    protected UIComponent doInit( UIComposite parent ) {
        ui = new PageContainer( this, parent );
        ui.cssClasses.add( "MessagesPage" );
        ui.title.set( abbreviate( title, 25 ) );

        selectedCard = Property.rw( ui, "selectedCard" );
        subject = Property.rw( ui, "subject" );

        ui.body.layout.set( new RowLayout().orientation.set( VERTICAL )
                .fillHeight.set( true )
                .fillWidth.set( true ) );

        // header
        headerContainer = ui.body.add( new UIComposite() {{
            layoutConstraints.set( new RowConstraints().height.set( 40 ) );

            layout.set( new RowLayout().orientation.set( VERTICAL ).margins.set( Size.of( 8, 3 ) ).fillHeight.set( true ).fillWidth.set( true ) );
            // margin
            add( new Text() {{
                cssClasses.add( "SubjectText" );
            }});
            // To:
            add( new Text() {{
                cssClasses.add( "SubjectText" );
                selectedCard.onInitAndChange( (newValue,__) -> {
                    if (newValue != null) {
                        var address = Address.parseEncoded( newValue.message.fromAddress.get() );
                        content.set( String.format( "To: %s", address.content ) );
                    }
                });
            }});
            // Subject:
            add( new UIComposite() {{
                layout.set( new RowLayout().fillHeight.set( true ).fillWidth.set( true ) );
                // Label
                add( new Text() {{
                    layoutConstraints.set( new RowConstraints().width.set( 40 ) );
                    cssClasses.add( "SubjectText" );
                    content.set( "Subject: " );
                }});
                // Link
                add( new Link() {{
                    cssClasses.add( "SubjectLink" );
                    selectedCard.onInitAndChange( (newValue,__) -> {
                        if (newValue != null) {
                            subject.set( newValue.message.threadSubject.opt().orElse( "???" ) );
                            content.set( abbreviate( subject.get(), 50 ) );
                        }
                    });
                    var link = this;
                    events.on( EventType.CLICK, ev -> {
                        components.remove( this );
                        components.add( new TextField() {{
                            layoutConstraints.set( new RowConstraints().width.set( 250 ) );
                            cssClasses.add( "SubjectLink" );
                            content.set( subject.get() );
                            Platform.schedule( 750, () -> focus.set( true ) );
                            events.on( EventType.SELECT, tev -> {
                                components.remove( this );
                                components.add( link );
                                subject.set( content.get() );
                                link.content.set( abbreviate( subject.get(), 50 ) );
                                layout();
                            });
                        }});
                        layout();
                    });
                }});
            }});
        }});

        // input field
        inputContainer = ui.body.add( new UIComposite() {{
            layoutConstraints.set( new RowConstraints().height.set( 38 ) );
            layout.set( new RowLayout().margins.set( Size.of( 8, 0 ) ).spacing.set( 8 ).fillHeight.set(  true ).fillWidth.set( true ) );
            messageTextInput = add( new TextField() {{
                cssClasses.add( "MessageTextInput" );
                //Platform.schedule( 2000, () -> focus.set( true ) );
            }});
            add( new Button() {{
                layoutConstraints.set( new RowConstraints().width.set( 70 ) );
                icon.set( "send" );
                tooltip.set( "Do it! :)" );
                events.on(  EventType.SELECT, ev -> {
                    send();
                    //messageTextInput.content.set( "" );
                });
            }});
        }});

        // typing...
        ui.body.add( new Text() {{
            layoutConstraints.set( new RowConstraints().height.set( 20 ) );
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
            layout.set( new MessagesLayout() {{
                provider.set( (startIndex, num) -> fetchMessages( startIndex, num ) );
            }});
        }});

        // update
        EventManager.instance()
                .subscribe( (ModelUpdateEvent ev) -> {
                    LOG.info( "Updating ..." );
                    messagesContainer.layout();
                })
                .performIf( ev -> ev instanceof ModelUpdateEvent
                        && !((ModelUpdateEvent)ev).entities( Message.class ).isEmpty() )
                .unsubscribeIf( () -> ui.isDisposed() );

        return ui;
    }


    protected Promise<List<MessageComponent>> fetchMessages( int startIndex, int num ) {
        var timer = Timer.start();
        return src.query()
                .firstResult( startIndex ).maxResults( num )
                .orderBy( Message.TYPE.date, Order.DESC )
                .executeCollect()
                .map( fetched -> {
                    LOG.info( "fetchMessages(): %d / %d -> %d (%s)", startIndex, num, fetched.size(), timer.elapsedHumanReadable() );
                    //LOG.info( "%s", Sequence.of( fetched ) );
                    var result = new ArrayList<MessageComponent>();
                    for (int i = 0; i < fetched.size(); i++) {
                        var msg = fetched.get( i );
                        var card = cards.computeIfAbsent( msg, __ -> new MessageCard( msg ) );
                        result.add( new MessageComponent( msg, card, i + startIndex ) );
                    }
                    return result;
                });
    }


    protected Promise<?> send() {
        Assert.that( selectedCard.opt().isPresent(), "selectedCard == null" );
        var msg = new TransportMessage() {{
            receipient = Address.parseEncoded( selectedCard.$().message.fromAddress.get() );
            text = messageTextInput.content.value();
            threadSubject = Opt.of( subject.get() );
            followUp = Opt.of( selectedCard.$().message );
        }};
        return ArecaApp.current()
                .transportFor( msg.receipient )
                .thenOpt( transport -> {
                    LOG.info( "Transport found for: %s - %s", msg.receipient, transport );
                    return transport.get().send( msg );
                })
                .onSuccess( result -> result
                        .ifPresent( sent -> {
                            LOG.info( "Transport sent! :) - %s", sent );
                            messageTextInput.content.set( "" );
                            EventManager.instance().publish( new MessageSentEvent( sent ) );
                        })
                        .ifAbsent( __ -> {
                            LOG.info( "No Transport!" );
                        })
                )
                .onError( ArecaApp.current().defaultErrorHandler() ); // XXX UI
    }


    /**
     *
     */
    protected class MessagesLayout
            extends DynamicLayoutManager<MessageComponent> {

        protected static final int  SPACING = 10;
        protected static final int  MARGINS = 8;

        protected Size              viewSize;

        protected List<UIComponent> separators = new ArrayList<>();

        protected Component         last;

        protected boolean           hasMore = true;

        protected boolean           isLayouting;


        @Override
        public void layout( UIComposite composite ) {
            boolean isFirstRun = scrollable == null;
            super.layout( composite );
            if (composite.clientSize.opt().isAbsent()) {
                return;
            }
            // remove previous lines (separators)
            Sequence.of( separators ).forEach( sep -> sep.dispose() );
            separators.clear();

            last = null;
            hasMore = true;
            viewSize = composite.clientSize.$(); //.substract( margins ).substract( margins );

            if (isFirstRun) {
                scrollable.scrollTop.onChange( (newValue,__) -> {
                    if (!isLayouting) {
                        checkVisibleMessages();
                    }
                    else {
                        LOG.info( "Skipping concurrent layout()" );
                    }
                });
            }
            checkVisibleMessages();
        }


        /**
         * Recursivly check if the current last loaded line is "below" the currently
         * scrolled view area. Wait for the current line to be layouted and recursivly
         * check the next line afterwards, so that the next line knows the height of
         * its predecessor.
         */
        protected void checkVisibleMessages() {
            LOG.info( "checkScroll(): scroll=%d, height=%d", scrollable.scrollTop.$(), viewSize.height() );
            int viewTop = scrollable.scrollTop.value();

            var startIndex = last != null ? last.index + 1 : 0;

            if (hasMore && lastBottom() - (viewSize.height()/2) < (viewTop + viewSize.height())) {
                isLayouting = true;
                provider.$().provide( startIndex, 5 ).onSuccess( loaded -> {
                    for (var c : loaded) {
                        var bottom = lastBottom();
                        LOG.debug( "Card: bottom=%d", bottom );
                        var card = (MessageCard)c.component;
                        if (card.parent() == null) {
                            scrollable.add( card );
                        }
                        var isOutgoing = c.message.outgoing.get();
                        card.position.set( Position.of(
                                isOutgoing ? 3*MARGINS : MARGINS, bottom + SPACING ) );
                        card.size.set( Size.of(
                                viewSize.width() - (MARGINS*4), card.computeMinHeight( viewSize.width() ) ) );
                        card.layout();
                        if (last == null) {  // select very first
                            card.select( true );
                        }
                        last = c;
                    }
                    hasMore = !loaded.isEmpty();
                    checkVisibleMessages();
                });
            }
            else {
                isLayouting = false;
            }
        }


        protected int lastBottom() {
            return last != null ? last.component.position.$().y + last.component.size.$().height() : 0;
        }


        @Override
        public void componentHasChanged( Component changed ) {
            throw new RuntimeException( "not yet implemented." );
        }
    }


    /**
     *
     */
    protected class MessageComponent
            extends Component {

        public Message      message;

        public MessageComponent( Message msg, UIComponent component, int index ) {
            super( component, index );
            this.message = msg;
        }
    }


    /**
     *
     */
    protected class MessageCard
            extends UIComposite
            implements Comparable<MessageCard> {

        private static final String CSS = "MessageCard";
        private static final String CSS_SELECTED = "MessageCard-selected";
        private static final String CSS_OUTGOING = "MessageCard-outgoing";
        private static final int    MARGINS = 10;
        private static final int    HEADER = 12;
        private static final int    SPACING = 5;
        private static final int    MAX_LINES = 12;

        public Message          message;

        protected boolean       isSelected;

        protected Text          contentText;


        public MessageCard( Message msg ) {
            this.message = msg;
            cssClasses.add( CSS );
            if (msg.outgoing.get()) {
                cssClasses.add( CSS_OUTGOING );
            }

            events.on( EventType.SELECT, ev -> {
                toggle();
            });

            var address = Address.parseEncoded( msg.outgoing.get() ? msg.toAddress.get() : msg.fromAddress.get() );
            var fromText = add( new Text() {{
                content.set( abbreviate( address.content, 30 ) );
                cssClasses.add( "FromText" );
            }});
            var dateText = add( new Text() {{
                content.set( df.format( new Date( msg.date.get()) ) );
                cssClasses.add( "DateText" );
            }});
            contentText = add( new Text() {{
                var decoded = msg.content.get();
                if (msg.contentType.get().equals( ContentType.HTML )) {
                    format.set( Format.HTML );
                    content.set( decoded );
                }
                else {
                    try {
                        format.set( Format.HTML );
                        var html = new StringBuilder( 4096 );
                        var reader = new BufferedReader( new StringReader( decoded ) );
                        var c = 0;
                        for (var line = reader.readLine(); line != null && c++ < MAX_LINES; line = reader.readLine()) {
                            html.append( line ).append( "<br/>" );
                        }
                        //LOG.info( "\nTEXT ---\n%s\nHTML ---\n%s", decoded, html.toString() );
                        content.set( html.toString() );
                    }
                    catch (IOException e) {
                        throw new RuntimeException( "Should never happen: " + e, e );
                    }
                }
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
                    contentText.size.set( Size.of( s.width(), s.height() - HEADER - SPACING - MARGINS ) );
                }
            });

            if (message.unread.get()) {
                new Badge( this ) {{
                    content.set( "X" );
                    message.onLifecycle( State.AFTER_REFRESH, ev -> {
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
            LOG.debug( "WIDHT: %s, charsPerLine: %s, lines: %s", width, charsPerLine, lines );
            lines = Math.min( MAX_LINES, lines );

            //contentText.content.set( abbreviate( message.content.get(), lines * charsPerLine ) );

            return (MARGINS*2) + HEADER + SPACING + (lines * 16) + (16/2); // half line displayed at the end
        }


        public void select( boolean select ) {
            //LOG.info( "Selected: %s, %s -> %s", message.content.get(), isSelected, select );
            // select
            if (!isSelected && select) {
                selectedCard.opt().ifPresent( it -> it.select( false ) );
                cssClasses.add( CSS_SELECTED );
                selectedCard.set( this );
                delayedMarkRead();
            }
            // unselect
            else if (isSelected && !select) {
                cssClasses.remove( CSS_SELECTED );
                selectedCard.set( null );
            }
            isSelected = select;
        }


        public void toggle() {
            select( !isSelected );
        }

        protected void delayedMarkRead() {
            if (message.unread.get()) {
                Platform.schedule( MESSAGE_MARK_READ_DELAY, () -> {
                    if (selectedCard.$() == this && message.unread.get()) {
                        var uow = ArecaApp.current().repo().newUnitOfWork();
                        uow.entity( message ).onSuccess( _message -> {
                            _message.unread.set( false );
                            uow.submit();
                        });
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
