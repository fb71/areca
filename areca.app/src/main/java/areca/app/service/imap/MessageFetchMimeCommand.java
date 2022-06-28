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
import java.io.BufferedReader;
import org.teavm.jso.json.JSON;

import areca.app.service.imap.ImapRequest.Command;
import areca.app.service.matrix.MatrixClient;

/**
 * Use Apache James and Blueglacier to parse MIME on the servlet.
 *
 * @author Falko Bräutigam
 */
public class MessageFetchMimeCommand extends Command {

    public StringBuilder    text = new StringBuilder( 4096 );

    public int              number;


    public MessageFetchMimeCommand( int number ) {
        this.number = number;
        command = format( "%s FETCH %d RFC822", tag, number );
        expected = format( "%s OK FETCH completed", tag );
    }


    @Override
    protected void parse( BufferedReader in ) throws Exception {
        in.readLine();
        super.parse( in );
        var js = JSON.parse( text.toString() );
        MatrixClient.console( js );
    }


    @Override
    protected boolean parseLine( String line ) {
        if (super.parseLine( line )) {
            text.append( line ).append( "\n" );
            return true;
        }
        return false;
    }


//    public static String toString( Reader input ) throws IOException {
//        int n = 0;
//        char[] buffer = new char[4096];
//        StringBuilder result = new StringBuilder( buffer.length);
//        while (-1 != (n = input.read( buffer ))) {
//            result.append( String.valueOf( buffer, 0, n ) );
//        }
//        return result.toString();
//    }

}