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
package areca.app.service.mail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;

import jakarta.activation.MimeType;
import jakarta.activation.MimeTypeParseException;
import jakarta.mail.Address;
import jakarta.mail.FetchProfile;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.search.AndTerm;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.ReceivedDateTerm;

/**
 *
 *
 * @author Falko Bräutigam
 */
public class MailServlet extends HttpServlet {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    protected Connections   connections = new Connections();


    @Override
    public void init() throws ServletException {
        log( "Init..." );
    }


    @Override
    public void destroy() {
        connections.dispose();
        super.destroy();
    }


    @Override
    public void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
        debug( "\n\nREQUEST: %s - %s", request.getPathInfo(), request.getParameterMap() );
        var timer = StopWatch.createStarted();

        // headers
        var params = new RequestParams();
        for (var header : Collections.list( request.getHeaderNames() )) {
            params.fromRequestHeader( header ).ifPresent( param -> {
                param.value = decode( request.getHeader( header ) );
            });
        }
        debug( "Headers: %s", params );

        // perform
        Object result = null;
        try {
            var file = decode( StringUtils.substringAfterLast( request.getPathInfo(), "/" ) );
            var path = decode( StringUtils.substringBeforeLast( request.getPathInfo(), "/" ) );
            var conn = connections.aquire( params );
            switch (file) {
                case AccountInfoRequest.FILE_NAME: result = new AccountInfo( conn, path ); break;
                case FolderInfoRequest.FILE_NAME: result = new FolderInfo( conn, path ); break;
                case MessageHeadersRequest.FILE_NAME: result = new MessageHeaders( conn, path, request ); break;
                case MessageContentRequest.FILE_NAME: result = new MessageContent( conn, path, request ); break;
                default: throw new RuntimeException( "Unknown file name: " + file );
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new ServletException( e );
        }
        // response
        response.setCharacterEncoding( "UTF-8" );
        gson.toJson( result, response.getWriter() );
        response.flushBuffer();
        debug( "REQUEST: done. (%s ms)", timer.getTime() );
    }


    /**
     *
     */
    protected static class AccountInfo {

        public String[] folderNames;

        protected AccountInfo( ImapConnection conn, String path ) throws MessagingException {
            this.folderNames = Arrays.stream( conn.store.getDefaultFolder().list( "*" ) )
                    .map( folder -> folder.getFullName() )
                    .toArray( String[]::new );
        }
    }


    /**
     *
     */
    protected static class FolderInfo {

        public boolean  exists;

        public int      count;

        public int      unread;

        protected FolderInfo( ImapConnection conn, String path ) throws MessagingException {
            var folder = conn.openFolder( path );
            this.count = folder.getMessageCount();
            this.unread = folder.getUnreadMessageCount();
            this.exists = folder.exists();
        }
    }


    /**
     *
     */
    protected static class MessageContent {

        public SingleMessageContent[] messageContent;

        protected MessageContent( ImapConnection conn, String path, HttpServletRequest request ) throws MessagingException, IOException, MimeTypeParseException {
            var folder = conn.openFolder( path );

            var numParam = request.getParameterValues( MessageContentRequest.NUM_NAME );
            var msgNums = Arrays.stream( numParam ).mapToInt( s -> Integer.valueOf( s ) ).toArray();

            Message[] msgs = folder.getMessages( msgNums );

            FetchProfile profile = new FetchProfile();
            profile.add( IMAPFolder.FetchProfileItem.MESSAGE );
            folder.fetch( msgs, profile );

            messageContent = new SingleMessageContent[ msgs.length ];
            for (int i = 0; i < msgs.length; i++) {
                var msg = (IMAPMessage)msgs[i];
                msg.setPeek( true );
                messageContent[i] = new SingleMessageContent();
                messageContent[i].messageNum = msg.getMessageNumber();
                messageContent[i].messageId = msg.getMessageID();
                messageContent[i].parts = messageParts( msg );
            }
        }

        protected List<MessagePart> messageParts( Part part ) throws IOException, MessagingException, MimeTypeParseException {
            try {
                Object content = part.getContent();
                // String
                if (content instanceof String) {
                    return Collections.singletonList( new MessagePart( part.getContentType(), (String)content ) );
                }
                // InputStream
                else if (content instanceof InputStream) {
                    var mimeType = new MimeType( part.getContentType() );
                    debug( "************************************************** MIME: %s", mimeType );
                    var charset = Optional
                            .ofNullable( mimeType.getParameter( "charset" ) )
                            .filter( it -> Charset.isSupported( it ) )
                            .orElse( "UTF-8" );
                    var s = new String( ((InputStream)content).readAllBytes(), charset );
                    return Collections.singletonList( new MessagePart( part.getContentType(), s ) );
                }
                // Multipart
                else if (content instanceof Multipart) {
                    Multipart multipart = (Multipart)content;
                    var result = new ArrayList<MessagePart>();
                    for (int i = 0; i < multipart.getCount(); i++) {
                        result.addAll( messageParts( multipart.getBodyPart( i ) ) );
                    }
                    return result;
                }
                else if (content instanceof MimeMessage) {
                    return messageParts( (MimeMessage)content );
                }
                else {
                    throw new RuntimeException( "Unknown part content type: " + content );
                }
            }
            // ...das ist auch nur "für den Fall" (..das Ornella Muti hereinspaziert, irgendwan...)
            catch (IOException | MessagingException | MimeTypeParseException e) {
                e.printStackTrace();
                return Collections.emptyList();
            }
        }

//        private List<InputStream> getAttachments( BodyPart part ) throws Exception {
//            var result = new ArrayList<InputStream>();
//            var content = part.getContent();
//            if (content instanceof InputStream || content instanceof String) {
//                if (Part.ATTACHMENT.equalsIgnoreCase( part.getDisposition() ) || StringUtils.isNotBlank( part.getFileName() )) {
//                    result.add( part.getInputStream() );
//                    return result;

        protected static class SingleMessageContent {
            public String messageId;
            public int messageNum;
            public List<MessagePart> parts;
        }

        protected static class MessagePart {
            public String type;
            public String content;

            protected MessagePart( String type, String content ) {
                this.type = type;
                this.content = content;
            }
        }
    }


    /**
     *
     */
    protected static class MessageHeaders {

        public SingleMessageHeaders[] messageHeaders;

        protected MessageHeaders( ImapConnection conn, String path, HttpServletRequest request ) throws MessagingException {
            var folder = conn.openFolder( path );
            var msgs = (Message[])null;

            // msg nums
            String minParam = request.getParameter( MessageHeadersRequest.MIN_NUM_NAME );
            String maxParam = request.getParameter( MessageHeadersRequest.MAX_NUM_NAME );
            if (minParam != null || maxParam != null) {
                var start = minParam != null ? Integer.parseInt( minParam ) : 1;
                var end = maxParam != null ? Integer.parseInt( maxParam ) : folder.getMessageCount();
                msgs = folder.getMessages( start, end );
            }
            // date
            minParam = request.getParameter( MessageHeadersRequest.MIN_DATE_NAME );
            maxParam = request.getParameter( MessageHeadersRequest.MAX_DATE_NAME );
            if (minParam != null || maxParam != null) {
                var term = new AndTerm(
                        new ReceivedDateTerm( ComparisonTerm.GE, new Date( Long.parseLong( minParam ) ) ),
                        new ReceivedDateTerm( ComparisonTerm.LE, new Date( Long.parseLong( maxParam ) ) ) );
                msgs = folder.search( term );
            }
            // all
            if (msgs == null) {
                //msgs = folder.getMessages();
                throw new MessagingException( "Wrong query: " + request.getParameterMap() );
            }

            FetchProfile profile = new FetchProfile();
            profile.add( FetchProfile.Item.ENVELOPE );
            profile.add( FetchProfile.Item.FLAGS );
            folder.fetch( msgs, profile );

            messageHeaders = new SingleMessageHeaders[ msgs.length ];
            for (int i = 0; i < msgs.length; i++) {
                messageHeaders[i] = new SingleMessageHeaders( (IMAPMessage)msgs[i] );
            }
        }

        protected static class SingleMessageHeaders {
            public String messageId;
            public int messageNum;
            public String subject;
            public long sentDate;
            public long receivedDate;
            public EmailAddress[] from;
            public EmailAddress[] to;
            public String[] flags;

            protected SingleMessageHeaders( IMAPMessage msg ) throws MessagingException {
                msg.setPeek( true );
                this.messageNum = msg.getMessageNumber();
                this.messageId = msg.getMessageID();
                this.subject = msg.getSubject();
                this.sentDate = msg.getSentDate().getTime();
                this.receivedDate = msg.getReceivedDate().getTime();
                this.from = stream( msg.getFrom() )
                        .map( a -> new EmailAddress( a ) ).toArray( EmailAddress[]::new );
                this.to = stream( msg.getRecipients( RecipientType.TO ) )
                        .map( a -> new EmailAddress( a ) ).toArray( EmailAddress[]::new );
                this.flags = new String[0];
                if (msg.getFlags().contains( Flags.Flag.SEEN )) {
                    flags = ArrayUtils.add( flags, "SEEN" );
                }
                if (msg.getFlags().contains( Flags.Flag.RECENT )) {
                    flags = ArrayUtils.add( flags, "RECENT" );
                }
                if (msg.getFlags().contains( Flags.Flag.DELETED )) {
                    flags = ArrayUtils.add( flags, "DELETED" );
                }
            }

            protected <R> Stream<R> stream( R[] a ) {
                return a != null ? Arrays.stream( a ) : Collections.<R>emptyList().stream();
            }
        }

        protected static class EmailAddress {
            public String type;
            public String personal;
            public String address;

            public EmailAddress( Address a ) {
                this.type = a.getType();
                this.address = ((InternetAddress)a).getAddress();
                this.personal = ((InternetAddress)a).getPersonal();
            }
        }
    }


    /**
     *
     */
    class ImapConnection {

        private Session                 session;

        protected Store                 store;

        protected Map<String,OpenFolder> openFolders = new ConcurrentHashMap<>();


        public ImapConnection( RequestParams params ) throws MessagingException {
            Properties props = new Properties();
            props.put( "mail.imap.ssl.enable", "true");
            props.put( "mail.imap.ssl.trust", params.host.value);
            props.put( "mail.imap.timeout", "7000");
            props.put( "mail.imap.connectiontimeout", "5000");
            //props.put("mail.smtp.starttls.enable", "true");

            session = Session.getInstance( props );
            //session.setDebug( true );

            // connects to the message store
            store = session.getStore( "imap" );
            store.connect( params.host.value, Integer.parseInt( params.port.value ),
                    params.username.value, params.password.value );
        }


        public Folder openFolder( String folderName ) {
            var result = openFolders.computeIfAbsent( folderName, __ -> {
                try {
                    debug( "POOL: Folder opened (%s)", folderName );
                    return new OpenFolder() {{
                        folder = store.getFolder( folderName );
                    }};
                }
                catch (MessagingException e) {
                    throw new RuntimeException( e );
                }
            }).touch().folder;

            if (!result.isOpen()) {
                try {
                    debug( "POOL: Folder (re-)opened (%s)", folderName );
                    result.open( Folder.READ_ONLY );
                }
                catch (MessagingException e) {
                    throw new RuntimeException( e );
                }
            }
            return result;
        }


        public void release() throws MessagingException {
            var now = System.currentTimeMillis();
            for (var folderName : new HashSet<>( openFolders.keySet() )) {
                var folder = openFolders.get( folderName );
                if (folder.lastUsed < (now-30000)) {
                    openFolders.remove( folderName );
                    if (folder.folder.isOpen()) {
                        folder.folder.close();
                        debug( "POOL: Folder closed (%s)", folderName );
                    }
                    else {
                        debug( "POOL: Folder already closed (%s)", folderName );
                    }
                }
            }
        }

        class OpenFolder {
            public Folder   folder;
            public long     lastUsed = System.currentTimeMillis();

            public OpenFolder touch() {
                lastUsed = System.currentTimeMillis();
                return this;
            }
        }
    }


    /**
     *
     */
    class Connections {

        private Map<RequestParams,ImapConnection>  all = new ConcurrentHashMap<>();

        private Thread                      scavenger;

        protected Connections() {
            scavenger = new Thread( () -> {
                while (scavenger != null) {
                    try {
                        Thread.sleep( 1000 );
                        //debug( "Connections: %d", all.size() );
                        for (var conn : all.values()) {
                            conn.release();
                        }
                    }
                    catch (Exception e) {
                        debug( "%s", e.getMessage() );
                        e.printStackTrace();
                    }
                }
                debug( "Scavenger: stopped." );
            }, "Mail Connections Scavenger"  );
            scavenger.setDaemon( true );
            scavenger.start();
        }


        protected void dispose() {
            this.scavenger = null;
        }


        public ImapConnection aquire( RequestParams params ) {
            return all.computeIfAbsent( params, __ -> {
                try {
                    debug( "POOL: New connection (size: %d)", all.size() );
                    return new ImapConnection( params );
                }
                catch (MessagingException e) {
                    throw new RuntimeException( e );
                }
            });
        }
    }


    protected String decode( String s ) {
        try {
            return URLDecoder.decode( s, "UTF-8" );
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException( e );
        }
    }


    protected static void debug( String msg, Object... args ) {
        System.out.println( args.length == 0 ? msg : String.format( msg, args ) );
    }

}
