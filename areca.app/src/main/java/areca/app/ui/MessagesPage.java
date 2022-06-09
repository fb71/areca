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
import java.util.HashMap;
import java.util.Map;

import java.nio.charset.Charset;
import java.text.DateFormat;

import org.apache.commons.lang3.StringUtils;
import org.apache.james.mime4j.codec.DecoderUtil;

import org.polymap.model2.ManyAssociation;
import org.polymap.model2.runtime.Lifecycle.State;

import areca.app.ArecaApp;
import areca.app.model.Address;
import areca.app.model.Message;
import areca.app.model.ModelSubmittedEvent;
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
import areca.ui.Align.Vertical;
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

    public static final int         MESSAGE_MARK_READ_DELAY = 5000;

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
            layout.set( new RowLayout() {{
                componentOrder.set( Comparator.<MessageCard>naturalOrder().reversed() );
                orientation.set( VERTICAL );
                fillWidth.set( true );
                spacing.set( 10 );
                margins.set( Size.of( 8, 1 ) );
            }});
        }});

        // update
        EventManager.instance()
                .subscribe( (ModelSubmittedEvent ev) -> {
                    LOG.info( "Model submitted" );
                    fetchMessages();
                })
                .performIf( ev -> ev instanceof ModelSubmittedEvent
                        && !((ModelSubmittedEvent)ev).entities( Message.class ).isEmpty() )
                .unsubscribeIf( () -> ui.isDisposed() );

        fetchMessages();
        return ui;
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


    protected void fetchMessages() {
        // XXX wait for repo to show up
        if (ArecaApp.instance().repo() == null) {
            LOG.info( "waiting for repo..." );
            Platform.schedule( 100, () -> fetchMessages() );
            return;
        }
        var timer = Timer.start();
        var chunk = new ArrayList<MessageCard>();

        src.fetch().onSuccess( (ctx,result) -> {
            result.ifPresent( msg -> {
                cards.computeIfAbsent( msg, __ -> {
                    var card = new MessageCard( msg );
                    chunk.add( card );
                    return card;
                });
            });

            // deferred layout chunk
            if (timer.elapsed( MILLISECONDS ) > timeout || ctx.isComplete()) {
                LOG.info( "" + timer.elapsedHumanReadable() );
                timer.restart();
                timeout = 1000;

                chunk.forEach( card -> messagesContainer.add( card ) );
                messagesContainer.layout();
                chunk.clear();

                // scrollIntoView last
                Sequence.of( cards.values() )
                        .reduce( (c1,c2) -> c1.compareTo( c2 ) < 0 ? c2 : c1 )
                        .ifPresent( last -> {
                            Platform.schedule( 1000, () -> {
                                last.scrollIntoView.set( Vertical.BOTTOM );
                                last.select( true );
                            });
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

            var address = Address.parseEncoded( msg.fromAddress.get() );
            var fromText = add( new Text() {{
                content.set( abbreviate( address.content, 30 ) );
                cssClasses.add( "FromText" );
            }});
            var dateText = add( new Text() {{
                content.set( df.format( new Date( msg.date.get()) ) );
                cssClasses.add( "DateText" );
            }});
            contentText = add( new Text() {{
                var decoded = DecoderUtil. decodeEncodedWords( msg.content.get(), Charset.forName( "ISO-8859-1" ) );
                content.set( StringUtils.abbreviate( decoded, 250 ) );
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
            LOG.debug( "WIDHT: %s, charsPerLine: %s, lines: %s", width, charsPerLine, lines );
            lines = Math.min( MAX_LINES, lines );

            contentText.content.set( abbreviate( message.content.get(), lines * charsPerLine ) );

            return (MARGINS*2) + HEADER + SPACING + (lines * 16);
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
