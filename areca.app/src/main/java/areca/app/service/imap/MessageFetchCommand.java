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

import static java.lang.String.format;
import static org.apache.james.mime4j.stream.EntityState.T_END_OF_STREAM;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;

import org.teavm.apachecommons.io.IOUtils;

import org.apache.james.mime4j.stream.MimeConfig;
import org.apache.james.mime4j.stream.MimeTokenStream;

import areca.app.service.imap.ImapRequest.Command;

/**
 *
 * @author Falko Bräutigam
 */
public class MessageFetchCommand extends Command {

    public StringBuilder    text = new StringBuilder( 4096 );

    public String           textContent;

    public String           htmlContent;


    public MessageFetchCommand( int number, String part ) {
        command = format( "%s FETCH %d (BODY[%s])", tag, number, part );
        expected = format( "%s OK FETCH completed", tag );
    }


    @Override
    protected void parse( BufferedReader in ) throws Exception {
        in.readLine();
        super.parse( in );

        MimeTokenStream s = new MimeTokenStream( MimeConfig.DEFAULT );
        s.parse( new ByteArrayInputStream( text.toString().getBytes( "UTF-8" ) ) );
        for (var state = s.getState(); state != T_END_OF_STREAM; state = s.next()) {
            // log.info( "State: " + state );
            switch (state) {
                case T_BODY : {
                    //log.info( "Body: contents = ..." + ", header data = " + s.getBodyDescriptor() );
                    switch (s.getBodyDescriptor().getMimeType()) {
                        case "text/plain" :
                            textContent = IOUtils.toString( s.getReader() );
                            break;
                        case "text/html" :
                            htmlContent = IOUtils.toString( s.getReader() );
                            break;
                    }
                    break;
                }
                default: {
                    //log.info( ":: " + s.getField() );
                    break;
                }
            }
        }
    }


    @Override
    protected boolean parseLine( String line ) {
        if (super.parseLine( line )) {
            text.append( line ).append( "\n" );
            return true;
        }
        return false;
    }
}