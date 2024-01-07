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
import java.util.stream.Collectors;
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
import jakarta.mail.Authenticator;
import jakarta.mail.FetchProfile;
import jakarta.mail.Flags;
import jakarta.mail.Flags.Flag;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.search.AndTerm;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.MessageIDTerm;
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
    protected void service( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException {
        debug( "\n\n%s: %s - %s", req.getMethod(), req.getPathInfo(), req.getParameterMap() );
        var timer = StopWatch.createStarted();

        // headers
        var params = new RequestParams();
        for (var header : Collections.list( req.getHeaderNames() )) {
            params.fromRequestHeader( header ).ifPresent( param -> {
                param.value = decode( req.getHeader( header ) );
            });
        }
        debug( "Headers: %s", params );

        // perform
        Object result = null;
        try {
            var file = decode( StringUtils.substringAfterLast( req.getPathInfo(), "/" ) );
            var path = decode( StringUtils.substringBeforeLast( req.getPathInfo(), "/" ) );
            switch (file) {
                case AccountInfoRequest.FILE_NAME:
                    result = new AccountInfo( connections.aquire( params ), path );
                    break;
                case FolderInfoRequest.FILE_NAME:
                    result = new FolderInfo( connections.aquire( params ), path );
                    break;
                case MessageHeadersRequest.FILE_NAME:
                    result = new MessageHeaders( connections.aquire( params ), path, req );
                    break;
                case MessageContentRequest.FILE_NAME:
                    result = new MessageContent( connections.aquire( params ), path, req );
                    break;
                case MessageSetFlagRequest.FILE_NAME:
                    result = new MessageSetFlag( connections.aquire( params ), path, req );
                    break;
                case MessageDeleteRequest.FILE_NAME:
                    result = new MessageDelete( connections.aquire( params ), path, req );
                    break;
                case MessageAppendRequest.FILE_NAME:
                    result = new MessageAppend( connections.aquire( params ), path, req );
                    break;
                case MessageSendRequest.FILE_NAME:
                    result = new MessageSend( params, path, req );
                    break;
                default: throw new RuntimeException( "Unknown file name: " + file );
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new ServletException( e );
        }
        // response
        resp.setContentType( "application/json" ); // charset=UTF-8" );
        resp.setCharacterEncoding( "UTF-8" );
        gson.toJson( result, resp.getWriter() );
        resp.flushBuffer();
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
    protected static class MessageSetFlag {

        public int count;

        protected MessageSetFlag( ImapConnection conn, String path, HttpServletRequest request ) throws MessagingException {
            var msgId = request.getParameter( MessageSetFlagRequest.ID_NAME );

            var folderNames = Arrays.stream( new AccountInfo( conn, path ).folderNames ).collect( Collectors.toList() );
            folderNames.remove( "INBOX" );
            folderNames.add( 0, "INBOX" );

            for (var folderName : folderNames) {
                var folder = conn.openFolder( folderName );
                var rs = folder.search( new MessageIDTerm( msgId ) );
                if (rs.length > 0) {
                    folder.close();
                    folder.open( Folder.READ_WRITE );
                    count = rs.length;
                    for (var msg : rs) {
                        msg.setFlag( Flag.SEEN, true );
                    }
                    folder.close( false );
                    break;
                }
            }
        }
    }


    /**
     *
     */
    protected static class MessageDelete {

        public int count = 0;

        protected MessageDelete( ImapConnection conn, String path, HttpServletRequest request ) throws MessagingException {
            var msgId = request.getParameter( MessageDeleteRequest.ID_NAME );

            var folderNames = Arrays.stream( new AccountInfo( conn, path ).folderNames ).collect( Collectors.toList() );
            folderNames.remove( "INBOX" );
            folderNames.add( 0, "INBOX" );

            for (var folderName : folderNames) {
                var folder = conn.openFolder( folderName );
                var rs = folder.search( new MessageIDTerm( msgId ) );
                if (rs.length > 0) {
                    folder.close();
                    folder.open( Folder.READ_WRITE );
                    count = rs.length;
                    var trash = conn.openFolder( "Trash" );
                    for (var msg : rs) {
                        folder.copyMessages( new Message[] {msg}, trash );
                        msg.setFlag( Flag.DELETED, true );
                    }
                    folder.close( true );
                    break;
                }
            }
        }
    }


    /**
     *
     */
    protected static class MessageAppend {

        public int count = 0;

        protected MessageAppend( ImapConnection conn, String path, HttpServletRequest request ) throws MessagingException, IOException {
            IMAPFolder folder = conn.openFolder( path );
            var msg = MessageSend.createMimeMessage( request, conn.session );
            folder.appendMessages( new MimeMessage[] {msg} );
            count = 1;
        }
    }


    /**
     *
     */
    protected static class MessageSend {

        public int count = 0;

        public String messageId;

        protected MessageSend( RequestParams params, String path, HttpServletRequest request ) throws Exception {
            Properties props = new Properties();

            var sf = new com.sun.mail.util.MailSSLSocketFactory();
            sf.setTrustAllHosts( true );
            //sf.setTrustedHosts(new String[] { "my-server" });

            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.ssl.checkserveridentity", "false" );
            props.put("mail.smtp.ssl.socketFactory", sf );
            //props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", params.host.value );
            props.put("mail.smtp.port", params.port.value );

            props.put( "mail.smtp.timeout", "7000");
            props.put( "mail.smtp.connectiontimeout", "5000");
            props.put( "mail.smtp.auth", "true" );

            var auth = new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication( params.username.value, params.password.value );
                }
            };

            var session = Session.getInstance( props, auth );
            //session.setDebug( true );

            MimeMessage out = createMimeMessage( request, session );
            Transport.send( out );
            messageId = out.getMessageID();
            count = 1;
        }

        public static MimeMessage createMimeMessage( HttpServletRequest request, Session session )
                throws IOException, MessagingException, AddressException {
            var msg = gson.fromJson( request.getReader(), Message.class );
            MimeMessage out = new MimeMessage( session );
            out.setSubject( msg.subject );
            out.setFrom( msg.from );
            out.setRecipients( MimeMessage.RecipientType.TO, InternetAddress.parse( msg.to ) );
            out.setText( msg.text, "UTF-8" );
            if (msg.messageId != null) {
                out.setHeader( "Message-ID", msg.messageId );
            }
            return out;
        }

        protected static class Message {
            public String subject;
            public String from;
            public String to;
            public String text;
            public String messageId;
        }
    }


    /**
     *
     */
    protected static class ImapConnection {

        private RequestParams           params;

        private Session                 session;

        protected Store                 store;

        protected long                  lastUsed = System.currentTimeMillis();

        protected Map<String,OpenFolder> openFolders = new ConcurrentHashMap<>();


        public ImapConnection( RequestParams params ) throws MessagingException {
            this.params = params;
            Properties props = new Properties();
            props.put( "mail.imap.ssl.enable", "true");
            props.put( "mail.imap.ssl.trust", "*" ); //params.host.value);
            props.put( "mail.imap.timeout", "7000");
            props.put( "mail.imap.connectiontimeout", "5000");
            //props.put("mail.smtp.starttls.enable", "true");

            session = Session.getInstance( props );
            //session.setDebug( true );

            // connects to the message store
            store = session.getStore( "imap" );
        }


        public ImapConnection checkOpen() throws MessagingException {
            if (!store.isConnected()) {
                synchronized (store) {
                    if (!store.isConnected()) {
                        debug( "POOL: Store opened" );
                        store.connect( params.host.value, Integer.parseInt( params.port.value ),
                                params.username.value, params.password.value );
                    }
                }
            }
            lastUsed = System.currentTimeMillis();
            return this;
        }


        public IMAPFolder openFolder( String folderName ) throws MessagingException {
            var result = openFolders.computeIfAbsent( folderName, __ -> {
                try {
                    debug( "POOL: created (%s)", folderName );
                    return new OpenFolder() {{
                        folder = store.getFolder( folderName );
                    }};
                }
                catch (MessagingException e) {
                    throw new RuntimeException( e );
                }
            }).touch().folder;

            if (!result.isOpen()) {
                synchronized (result) {
                    if (!result.isOpen()) {
                        debug( "POOL: Folder (re-)opened (%s)", folderName );
                        result.open( Folder.READ_ONLY );
                    }
                }
            }
            return (IMAPFolder)result;
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
            if (openFolders.isEmpty() && store.isConnected() && lastUsed < (now-30000)) {
                synchronized (store) {
                    if (store.isConnected()) {
                        debug( "POOL: Store closed." );
                        store.close();
                    }
                }
            }
        }


        protected static class OpenFolder {
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


        public ImapConnection aquire( RequestParams params ) throws MessagingException {
            return all.computeIfAbsent( params, __ -> {
                try {
                    debug( "POOL: New connection (size: %d)", all.size() );
                    return new ImapConnection( params );
                }
                catch (MessagingException e) {
                    throw new RuntimeException( e );
                }
            }).checkOpen();
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
