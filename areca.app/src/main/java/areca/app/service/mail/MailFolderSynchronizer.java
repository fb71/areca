/*
 * Copyright (C) 2020-2022, the @authors. All rights reserved.
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
package areca.app.service.mail;

import static org.polymap.model2.query.Expressions.eqAny;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.time.DateUtils;

import org.polymap.model2.query.Expressions;
import org.polymap.model2.runtime.UnitOfWork;

import areca.app.model.Anchor;
import areca.app.model.Message;
import areca.app.model.Message.ContentType;
import areca.app.service.mail.MessageHeadersRequest.MessageHeadersResponse.MessageHeaders;
import areca.common.Assert;
import areca.common.Promise;
import areca.common.base.Consumer.RConsumer;
import areca.common.base.Opt;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * Synchronizes an entire IMAP folder with the datebase.
 *
 * @author Falko Bräutigam
 */
public class MailFolderSynchronizer {

    private static final Log LOG = LogFactory.getLog( MailFolderSynchronizer.class );

    //public static final int             MAX_PER_FOLDER = 40;

    protected RequestParams             params;

    protected int                       monthsToSync;

    protected UnitOfWork                uow;

    protected String                    folderName;

    private Opt<Anchor>                 folderAnchor = Opt.absent();

    private RConsumer<Integer>          onMessageCount;


    public MailFolderSynchronizer( String folderName, UnitOfWork uow, RequestParams params, int months ) {
        this.uow = uow;
        this.params = params;
        this.folderName = folderName;
        this.monthsToSync = months;
    }


    public MailFolderSynchronizer onMessageCount( RConsumer<Integer> consumer ) {
        this.onMessageCount = consumer;
        return this;
    }


    public Promise<Opt<Message>> start() {
        // query imap messages
        return fetchMessageIds()
                // check/create Anchor
                .then( (MessageHeaders[] msgs) -> {
                    onMessageCount.accept( msgs.length );
                    if (msgs.length == 0
                            || folderName.equalsIgnoreCase( "INBOX" ) || folderName.equalsIgnoreCase( "Sent" )) {
                        LOG.debug( "%s: no messages to sync -> skipping Anchor", folderName );
                        return Promise.completed( msgs );
                    }
                    return checkCreateFolderAnchor().map( anchor -> {
                        folderAnchor = Opt.of( anchor );
                        return msgs;
                    });
                })
                // find missing Message entities
                .then( (MessageHeaders[] msgs) -> {
                    LOG.debug( "%s: Ids: %s", folderName, msgs.length );
                    var msgIds = Sequence.of( msgs ).reduce( new HashMap<String,MessageHeaders>(), (map,msg) -> {
                        if (map.putIfAbsent( msg.messageId(), msg ) != null) {
                            LOG.info( "Message-ID: %s already exists!", msg.messageId() );
                        }
                        return map;
                    });
                    // query Messages that are in the store
                    return uow.query( Message.class )
                            .where( eqAny( Message.TYPE.storeRef, msgIds.keySet() ) )
                            .execute()
                            .reduce( msgIds, (result,opt) -> { // reduce to not found
                                opt.ifPresent( entity -> result.remove( entity.storeRef.get() ) );
                            })
                            .map( map -> map.values() );
                })
                .onSuccess( missingMsg -> {
                    LOG.info( "%s: Missing: %s", folderName, missingMsg.size() );
                })
                // fetch IMAP messages
                .then( missingMsg -> {
                    if (missingMsg.isEmpty()) {
                        LOG.debug( "%s: sending absent()", folderName );
                        return Promise.absent();
                    }
                    else {
                        var it = missingMsg.iterator();
                        return Promise.serial( missingMsg.size(), i -> fetchMessage( it.next() ) )
                                .map( msg -> Opt.of( msg ) );
                    }
                })
                .onSuccess( (Opt<Message> msg) -> {
                    LOG.debug( "%s: message: %s", folderName, msg );
                    msg.ifPresent( m -> {
                        folderAnchor.ifPresent( it -> it.messages.add( m ) );
                    });
                });
    }


    protected Promise<Anchor> checkCreateFolderAnchor() {
        var storeRef = "imap-folder:" + folderName;
        return uow.query( Anchor.class )
                .where( Expressions.eq( Anchor.TYPE.storeRef, storeRef ) )
                .executeCollect()
                .map( anchors -> {
                    if (anchors.isEmpty()) {
                        return uow.createEntity( Anchor.class, proto -> {
                            proto.storeRef.set( storeRef );
                            proto.name.set( folderName.replace( "/", " " ) );
                        });
                    }
                    else {
                        Assert.isEqual( 1, anchors.size() );
                        return anchors.get( 0 );
                    }
                });
    }


    protected Promise<Integer> fetchMessageCount() {
        return new FolderInfoRequest( params, folderName ).submit()
                .map( folderInfo -> folderInfo.count() );
    }


    protected Promise<MessageHeaders[]> fetchMessageIds( int start, int end ) {
        return new MessageHeadersRequest( params, folderName, Range.between( start, end ) )
                .submit()
                .map( response -> response.messageHeaders() );
    }


    protected Promise<MessageHeaders[]> fetchMessageIds() {
        var minDate = DateUtils.addMonths( new Date(), -monthsToSync );
        LOG.debug( "MIN. DATE: %s", minDate );
        return new MessageHeadersRequest( params, folderName, minDate, null )
                .submit()
                .map( response -> response.messageHeaders() );
    }


    protected Promise<Message> fetchMessage( MessageHeaders msg ) {
        LOG.info( "%s: Fetching: %s", folderName, msg.messageNum() );
        return new MessageContentRequest( params, folderName, Collections.singleton( msg.messageNum() ) )
                .submit()
                .map( response -> {
                    Assert.isEqual( 1, response.messageContent().length );
                    return uow.createEntity( Message.class, proto -> {
                        proto.storeRef.set( msg.messageId() );
                        if (msg.from().length > 0) { // XXX skip message without From: ?
                            proto.fromAddress.set( new EmailAddress( msg.from()[0].address() ).encoded() );
                            proto.replyAddress.set( new EmailAddress( msg.from()[0].address() ).encoded() );  // XXX ReplyTo:
                        }
                        if (msg.to().length > 0) { // XXX skip message without To: ?
                            proto.toAddress.set( new EmailAddress( msg.to()[0].address() ).encoded() );
                        }
                        proto.threadSubject.set( strippedSubject( msg.subject().opt().orElse( "" ) ) );
                        proto.unread.set( !msg.flags().contains( "SEEN" ) );
                        proto.date.set( msg.receivedDate().getTime() );
                        proto.outgoing.set( folderName.equalsIgnoreCase( "Sent" ) );

                        var content = response.messageContent()[0];
                        content.bodyParts( "text/plain", "text/html" ).ifPresent( body -> {
                            proto.content.set( body.content() );
                            proto.contentType.set( body.isType( "text/plain" ) ? ContentType.PLAIN : ContentType.HTML );
                        });
                    });
                });
    }


    public static final Pattern SUBJECT_PREFIX = Pattern.compile( "[^:]+:\\s*" );

    protected static String strippedSubject( String prefixed ) {
        if (prefixed == null) {
            return null;
        }
        var matcher = SUBJECT_PREFIX.matcher( prefixed );
        var start = 0;
        for (; matcher.find( start ); start = matcher.end()) {
            if (matcher.end() - start > 5) {
                break;
            }
        }
        return prefixed.substring( start );
    }
}
