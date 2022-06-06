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
package areca.app.service.imap;

import static areca.app.service.imap.ImapRequest.DEFAULT_ENCODING;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.substringAfter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import areca.app.service.TransportService.TransportMessage;
import areca.app.service.imap.ImapRequest.Command;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public class AppendCommand {

    private static final Log LOG = LogFactory.getLog( AppendCommand.class );

    public static final String  CRLF = "\r\n";

    public List<Command>        commands = new ArrayList<>();


    public AppendCommand( String folderName, TransportMessage msg, String from ) {
        String encoded = encode( msg, from );

        commands.add( new Command() {{
            command = format( "%s APPEND \"%s\" {%d}", tag, folderName, encoded.length() );
            expected = "+";
        }});
        commands.add( new Command() {{
            command = encoded;
            expected = "OK";
        }});
    }


    @SuppressWarnings("deprecation")
    protected String encode( TransportMessage msg, String from ) {
        var s = new StringBuilder( 4096 );
        s.append( "Subject: " ).append( msg.threadSubject.orElse( "" ) ).append( CRLF ); // XXX encode!
        s.append( "From: " ).append( from ).append( CRLF );
        s.append( "To: " ).append( msg.receipient.content ).append( CRLF );
        s.append( "Content-Type: text/plain; charset=" ).append( DEFAULT_ENCODING ).append( CRLF );
        // Content-Transfer-Encoding: 7bit
        s.append( "Date: " ).append( new Date().toGMTString() ).append( CRLF );
        s.append( "Message-ID: ")
                .append( format( "<%s@%s>", UUID.randomUUID(), substringAfter( from, "@") ) )
                .append( CRLF );
        s.append( CRLF );
        s.append( msg.text );
        s.append( CRLF );
        return s.toString();
    }

}
