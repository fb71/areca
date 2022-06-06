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
package areca.app.service.smtp;

import static java.lang.String.format;

import org.teavm.classlib.impl.Base64Impl;

import areca.app.service.imap.ImapRequest;
import areca.common.base.Consumer;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * The SMTP client.
 *
 * @author Falko Bräutigam
 */
public class SmtpRequest
        extends ImapRequest {

    private static final Log LOG = LogFactory.getLog( SmtpRequest.class );

    public <E extends Exception> SmtpRequest( Consumer<ImapRequest,E> initializer ) throws E {
        super( initializer );
    }


    public static class HeloCommand extends LoginCommand {
        public HeloCommand( String senderDomain ) {
            super( "fake", "fake" );
            command = String.format( "HELO %s", senderDomain );
            expected = "250";
        }
    }


    /**
     * https://www.samlogic.net/articles/smtp-commands-reference-auth.htm
     * https://www.atmail.com/blog/smtp-101-manual-smtp-sessions/
     */
    public static class AuthPlainCommand extends Command {
        public AuthPlainCommand( String username, String password ) {
            var encoded = Base64Impl.encode( String.format("\0%s\0%s", username, password ).getBytes(), false );
            command = String.format( "AUTH PLAIN %s", new String( encoded ) );
            expected = "235";
        }
    }


    public static class MailFromCommand extends Command {
        public MailFromCommand( String from ) {
            command = String.format( "MAIL FROM: <%s>", from );
            expected = "250";
        }
    }


    public static class RcptToCommand extends Command {
        public RcptToCommand( String to ) {
            command = String.format( "RCPT TO: <%s>", to );
            expected = "250";
        }
    }


    public static class DataCommand extends Command {
        public DataCommand() {
            command = String.format( "DATA" );
            expected = "354";
        }
    }


    public static class DataContentCommand extends Command {
        public DataContentCommand( String subject, String body ) {
            command = format( "Subject: %s\r\n", subject, body );
            command += format( "Content-Type: text/plain; charset=%s\r\n", DEFAULT_ENCODING );
            command += String.format( "%s\r\n.", body ); // last CRLF is added by servlet
            expected = "250";
        }
    }


    public static class QuitCommand extends Command {
        public QuitCommand() {
            command = "quit";
            expected = "221";
        }
    }

}
