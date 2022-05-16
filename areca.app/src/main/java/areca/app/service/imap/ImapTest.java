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
import static areca.common.base.With.with;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.Range.between;
import static org.polymap.model2.store.tidbstore.IDBStore.nextDbVersion;

import java.util.concurrent.TimeUnit;

import org.teavm.jso.json.JSON;

import org.apache.commons.lang3.mutable.MutableInt;

import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.store.tidbstore.IDBStore;
import areca.app.model.Anchor;
import areca.app.model.Contact;
import areca.app.model.Message;
import areca.app.service.imap.ImapRequest.LoginCommand;
import areca.common.Assert;
import areca.common.NullProgressMonitor;
import areca.common.Promise;
import areca.common.Timer;
import areca.common.base.Sequence;
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

    private static final Log LOG = LogFactory.getLog( ImapTest.class );

    public static final ImapTestClassInfo info = ImapTestClassInfo.instance();

    private static int          dbCount = 0;

    protected Promise<EntityRepository> initRepo( String name ) {
        return EntityRepository.newConfiguration()
                .entities.set( asList( Message.info, Contact.info, Anchor.info) )
                .store.set( new IDBStore( "ImapTest-" + name, nextDbVersion(), true ) )
                .create();
    }

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
            LOG.info( json );
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
                .catchError( e -> LOG.info( "Error" + e ) )
                .onSuccess( v -> {
                    LOG.info( "Async: "  + v );
                    //throw new Exception();
                })
                .waitForResult( v -> {
                    LOG.info( "Wait: "  + v );
                });

        Assert.that( timer.elapsed( TimeUnit.MILLISECONDS ) > 1000 );
    }


    @Test
    @Skip
    public Promise<?> loginTest() {
        request.commands.add( new FolderListCommand() );
        return request.submit().onSuccess( command -> {
            with( command ).instanceOf( FolderListCommand.class, flc -> {
                Assert.that( flc.folderNames.contains( "INBOX" ) );
            });
        });
    }


    @Test
    @Skip
    public Promise<?> selectFolderTest() {
        request.commands.add( new FolderSelectCommand( "INBOX" ) );
        return request.submit().onSuccess( command -> {
            with( command ).instanceOf( FolderSelectCommand.class, fsc -> {
                LOG.info( "INBOX: %s/%s", fsc.exists, fsc.recent );
                Assert.that( fsc.exists > -1 );
                Assert.that( fsc.recent > -1 );
            });
        });
    }


    @Test
    public Promise<?> fetchMessageTest() {
        request.commands.add( new FolderSelectCommand( "INBOX" ) );
        request.commands.add( new MessageFetchCommand( 1, "TEXT" ) );
        return request.submit().onSuccess( command -> {
            with( command ).instanceOf( MessageFetchCommand.class, fetchCommand -> {
                var text = fetchCommand.text.toString();
                Assert.that( text.length() > 0 );

                LOG.info( "Text: " + fetchCommand.textContent );
                LOG.info( "HTML: " + fetchCommand.htmlContent );
            });
        });
    }


    @Test
    public Promise<?> fetchMessageHeadersTest() {
        request.commands.add( new FolderSelectCommand( "INBOX" ) );
        request.commands.add( new MessageFetchHeadersCommand( between( 1, 2 ), SUBJECT, FROM, TO, DATE, MESSAGE_ID ) );
        return request.submit().onSuccess( command -> {
            with( command ).instanceOf( MessageFetchHeadersCommand.class, fetchCommand -> {
                for (var entry : fetchCommand.headers.entrySet()) {
                    LOG.info( "HEADER: %s: %s", entry.getKey(), entry.getValue() );
                    Assert.isEqual( 5, entry.getValue().size() );
                }
            });
        });
    }


    @Test
    @Skip
    public Promise<?> poolTest() {
        var count = new MutableInt();
        return Sequence.ofInts( 1, 10 )
                .map( i -> {
                    var r = newRequest();
                    r.commands.add( new FolderSelectCommand( "INBOX" ) );
                    return r.submit();

                })
                .reduce( (p1, p2) -> p1.join( p2 ) ).get()
                .onSuccess( (promise,commands) -> {
                    count.increment();
                    if (promise.isComplete()) {
                        Assert.isEqual( 10, count.intValue() );
                    }
                });
    }


    @Test
    public Promise<?> syncFolderTest() {
        return initRepo( "syncFolder" ).then( repo -> {
            return new ImapFolderSynchronizer( "Test1", repo, () -> newRequest(), new NullProgressMonitor() )
                    .start();
                    //.onSuccess( (self,msg) );
        });
    }
}
