/*
 * Copyright (C) 2021, the @authors. All rights reserved.
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
package areca.app.service.imap;

import static areca.app.service.imap.MessageFetchHeadersCommand.FieldEnum.DATE;
import static areca.app.service.imap.MessageFetchHeadersCommand.FieldEnum.FROM;
import static areca.app.service.imap.MessageFetchHeadersCommand.FieldEnum.MESSAGE_ID;
import static areca.app.service.imap.MessageFetchHeadersCommand.FieldEnum.SUBJECT;
import static areca.app.service.imap.MessageFetchHeadersCommand.FieldEnum.TO;
import static org.apache.commons.lang3.Range.between;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.teavm.jso.json.JSON;

import areca.app.service.imap.ImapRequest.LoginCommand;
import areca.common.Assert;
import areca.common.Promise;
import areca.common.Timer;
import areca.common.base.With;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.RuntimeInfo;
import areca.common.testrunner.Skip;
import areca.common.testrunner.Test;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class ImapTest {

    private static final Log log = LogFactory.getLog( ImapTest.class );

    public static final ImapTestClassInfo info = ImapTestClassInfo.instance();

//    protected static Lazy.$<EntityRepository> repo = new Lazy.$<>( () -> {
//        log.info( "Creating 'ImapTest' repo..." );
//        return EntityRepository.newConfiguration()
//                .entities.set( Arrays.asList( Message.info, Contact.info, Anchor.info) )
//                .store.set( new IDBStore( "ImapTest", 1, true ) )
//                .create();
//    });

    protected ImapRequest   request = newRequest();


    protected ImapRequest newRequest() {
        return new ImapRequest( self -> {
            self.host = "mail.polymap.de";
            self.port = 993;
            self.loginCommand = new LoginCommand( "areca@polymap.de", "dienstag" );
        });
    }


    @Test
    public void jsonTest() {
        request.commands.add( new FolderListCommand() );
        request.commands.add( new MessageFetchCommand( 1, "1" ) );

        With.$( request.toJson() ).apply( json -> {
            log.info( json );
            JSON.parse( json );
            // Window.alert( parsed );
        } );
    }


    @Test
    public void completablePromiseTest() {
        var promise = new Promise.Completable<String>();

        var timer = Timer.start();
        new Thread( () -> {
            try { Thread.sleep( 1000 ); } catch (InterruptedException e) { }

            //promise.completeWithError( new RuntimeException("") );
            promise.complete( "" + System.currentTimeMillis() );
        }).start();

        promise
                .catchError( e -> log.info( "Error" + e ) )
                .then( v -> {
                    log.info( "Async: "  + v );
                    //throw new Exception();
                })
                .thenWait( v -> {
                    log.info( "Wait: "  + v );
                });

        Assert.that( timer.elapsed( TimeUnit.MILLISECONDS ) > 1000 );
    }


    @Test
    public void loginTest() {
        request.commands.add( new FolderListCommand() );
        request.submit().thenWait( commands -> {
            Assert.that( ((FolderListCommand)commands[0]).folderNames.contains( "INBOX" ) );
        });
    }


    @Test
    public void fetchMessageTest() {
        request.commands.add( new FolderSelectCommand( "INBOX" ) );
        request.commands.add( new MessageFetchCommand( 3, "TEXT" ) );
        request.submit().thenWait( commands -> {
            var fetchCommand = (MessageFetchCommand)commands[1];
            var text = fetchCommand.text.toString();
            Assert.that( text.length() > 0 );

            log.info( "Text: " + fetchCommand.textContent );
            log.info( "HTML: " + fetchCommand.htmlContent );
        });
    }


    @Test
    public void fetchMessageHeadersTest() {
        request.commands.add( new FolderSelectCommand( "INBOX" ) );
        request.commands.add( new MessageFetchHeadersCommand( between( 1, 3 ), FROM, SUBJECT, TO, DATE, MESSAGE_ID ) );
        request.submit().thenWait( commands -> {
            var fetchCommand = (MessageFetchHeadersCommand)commands[1];
            for (var entry : fetchCommand.headers.entrySet()) {
                log.info( "%s: %s", entry.getKey(), entry.getValue() );
            }
        });
    }


    @Test
    @Skip
    public void poolTest() throws InterruptedException {
        var count = 10;
        AtomicInteger result = new AtomicInteger( 0 );
        for (int i = 0; i < count; i++) {
            var r = newRequest();
            r.commands.add( new FolderSelectCommand( "INBOX" ) );
            r.submit().then( commands -> {
                synchronized (result) {
                    result.incrementAndGet();
                    result.notifyAll();
                }
            });
        }
        while (result.get() < count) {
            synchronized (result) {
                result.wait();
            }
        }
    }

}
