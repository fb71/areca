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

import java.util.ArrayList;
import java.util.List;

import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public class ImapRequestData {

    private static final Log log = LogFactory.getLog( ImapRequestData.class );

    // public static final String RESPONSE_COMMAND_DELIMITER = "### command-end ########################################\n";


//    public <E extends Exception> ImapRequestData( Consumer<ImapRequestData,E> initializer ) throws E {
//        initializer.accept( this );
//    }

    public String               host;

    public int                  port;

    public CommandData          loginCommand;

    public List<CommandData>    commands = new ArrayList<>();


    protected String toJson() {
        var out = new StringBuilder( 4096 );
        out.append( "{\n" );
        out.append( "  \"host\": \"" + host + "\",\n" );
        out.append( "  \"port\": \"" + port + "\",\n" );
        out.append( "  \"loginCommand\": " ).append( loginCommand.toJson() ).append( ",\n" );
        out.append( "  \"commands\": [\n" );
        out.append( "    " ).append( String.join( ",\n", Sequence.of( commands ).map( c -> c.toJson() ).asIterable() ) );
        out.append( "  \n]\n" );
        out.append( "}\n" );
        return out.toString();
    }


    protected static String quote(String s) {
        return s.replace( "\"", "\\\"" );
    }


    public static class CommandData {

        public String       command;

        public String       expected;

        protected String toJson() {
            var out = new StringBuilder( 1024 );
            out.append( "{" );
            out.append( "\"command\": \"" + quote( command ) + "\", " );
            out.append( "\"expected\": \"" + quote( expected ) + "\"" );
            out.append( "}" );
            return out.toString();
        }
    }

}
