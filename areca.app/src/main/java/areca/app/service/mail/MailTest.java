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
package areca.app.service.mail;

import static java.util.Arrays.asList;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;

import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.time.DateUtils;

import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.store.tidbstore.IDBStore;

import areca.app.model.Anchor;
import areca.app.model.Contact;
import areca.app.model.ImapSettings;
import areca.app.model.Message;
import areca.app.service.SyncableService;
import areca.app.service.mail.MessageHeadersRequest.MessageHeadersResponse.MessageHeaders;
import areca.common.Assert;
import areca.common.NullProgressMonitor;
import areca.common.Promise;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.common.testrunner.Skip;
import areca.common.testrunner.Test;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class MailTest {

    private static final Log LOG = LogFactory.getLog( MailTest.class );

    public static final ClassInfo<MailTest> info = MailTestClassInfo.instance();

    protected RequestParams defaultParams() {
        var params = new RequestParams();
        params.host.value = "mail.polymap.de";
        params.username.value = "areca@polymap.de";
        params.password.value = "dienstag";
        params.port.value = "993";
        return params;
    }

    protected Promise<EntityRepository> initRepo( String name ) {
        return EntityRepository.newConfiguration()
                .entities.set( asList( Message.info, Contact.info, Anchor.info, ImapSettings.info ) )
                .store.set( new IDBStore( "MailTest-" + name, IDBStore.nextDbVersion(), true ) )
                .create();
    }


    @Test
    @Skip
    public Promise<?> accountInfoTest() {
        return new AccountInfoRequest( defaultParams() ).submit()
                .onSuccess( accountInfo -> {
                    LOG.info( "Folders: %s", Arrays.asList( accountInfo.folderNames() ) );
                });
    }


    @Test
    @Skip
    public Promise<?> folderInfoTest() {
        return new FolderInfoRequest( defaultParams(), "Test1/subtest" ).submit()
                .onSuccess( folderInfo -> {
                    LOG.info( "Count: %s", folderInfo.count() );
                    LOG.info( "Unread: %s", folderInfo.unread() );
                });
    }


    @Test
    @Skip
    public Promise<?> syncFolderTest() {
        return initRepo( "syncFolder" ).then( repo -> {
            var uow = repo.newUnitOfWork();
            return new MailFolderSynchronizer( "Test1", uow, defaultParams(), 36 )
                    .onMessageCount( msgCount -> LOG.info( "fetching: %s", msgCount ) )
                    .start()
                    .reduce( new MutableInt(), (r,msg) -> r.increment())
                    .map( count -> {
                        //Assert.isEqual( 10, count.intValue() );
                        return uow.submit().onSuccess( submitted -> {
                            LOG.info( "Submitted: %s", count );
                        });
                    });
        });
    }


    @Test
    public Promise<?> syncServiceTest() {
        return initRepo( "syncService" ).then( repo -> {
            var uow = repo.newUnitOfWork();
            var settings = uow.createEntity( ImapSettings.class, proto -> {
                var params = defaultParams();
                proto.username.set( params.username.value );
                proto.pwd.set( params.password.value );
                proto.host.set( params.host.value );
                proto.port.set( Integer.parseInt( params.port.value ) );
                proto.monthsToSync.set( 36 );
            });
            var ctx = new SyncableService.SyncContext() {{
                monitor = new NullProgressMonitor();
                uowFactory = () -> repo.newUnitOfWork();
            }};
            return new MailService.FullSync( settings, ctx ).start();
        });
    }


    @Test
    @Skip
    public Promise<?> messageContentTest() {
        HashSet<Integer> msgNums = Sequence.ofInts( 1, 47 ).toSet();
        return new MessageContentRequest( defaultParams(), "Test1", msgNums ).submit()
                .onSuccess( response -> {
                    Assert.isEqual( 47, response.messageContent().length );
                    for (var msg : response.messageContent() ) {
                        Assert.notNull( msg.messageId() );
                        Assert.that( msg.messageNum() > 0 );
                        for (var part : msg.parts()) {
                            Assert.notNull( part.type() );
                            Assert.notNull( part.content() );
                            LOG.info( "Msg: %d - %s (%s)", msg.messageNum(), part.type(), part.isType( "text/plain" ) );
                        }
                    }
                });
    }


    @Test
    @Skip
    public Promise<?> messageHeadersTest() {
        return new MessageHeadersRequest( defaultParams(), "Test1", Range.between( 1, 47 ) ).submit()
                .onSuccess( response -> {
                    MessageHeaders[] messageHeaders = response.messageHeaders();
                    Assert.isEqual( 47, messageHeaders.length );
                    Sequence.of( messageHeaders ).forEach( msg -> {
                        Assert.notNull( msg.messageId() );
                        Assert.that( msg.messageNum() > 0 );
                        Assert.notNull( msg.subject().opt().orElse( null ) );
                        Assert.notNull( msg.sentDate() );
                        Assert.notNull( msg.receivedDate() );
                        Assert.notNull( msg.from()[0].address() );
                        Assert.notNull( msg.to()[0].address() );
                        LOG.info( "To: (%s) %s", msg.to()[0].personal().opt().orElse( "-" ), msg.to()[0].address() );
                        LOG.info( "Flags: %s (%s)", msg.flags(), msg.flags().contains( "SEEN" ) );
                    });
                });
    }


    @Test
    public Promise<?> messagesDateQueryTest() {
        var min = DateUtils.addYears( new Date(), -2 );
        return new MessageHeadersRequest( defaultParams(), "Test1", min, null ).submit()
                .onSuccess( response -> {
                    MessageHeaders[] messageHeaders = response.messageHeaders();
                    Assert.that( messageHeaders.length > 0 );
                    Sequence.of( messageHeaders ).forEach( msg -> {
                        Assert.notNull( msg.receivedDate().compareTo( min ) >= 0 );
                        LOG.info( "Received: %s", msg.receivedDate() );
                    });
                });
    }



    @Test
    @Skip
    public Promise<?> concurrentTest() {
        return Promise.joined( 10, i -> new FolderInfoRequest( defaultParams(), "INBOX" ).submit() )
                .onSuccess( folderInfo -> {
                    LOG.info( "Count: %s", folderInfo.count() );
                    LOG.info( "Unread: %s", folderInfo.unread() );
                });
    }

}
