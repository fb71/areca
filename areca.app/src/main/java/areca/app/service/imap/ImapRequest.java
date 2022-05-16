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

import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.leftPad;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import areca.common.Platform;
import areca.common.Promise;
import areca.common.Timer;
import areca.common.base.Consumer;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * <p>
 * https://www.atmail.com/blog/imap-commands/
 *
 * @author Falko Bräutigam
 */
public class ImapRequest
        extends ImapRequestData {

    private static final Log LOG = LogFactory.getLog( ImapRequest.class );


    public <E extends Exception> ImapRequest( Consumer<ImapRequest,E> initializer ) throws E {
        initializer.accept( this );
    }


    public Promise<Command> submit() {
        var timer = Timer.start();
        return Platform.xhr( "POST", "imap/" )
                //.onReadyStateChange( state -> LOG.info( "Request ready state: " + state ) )
                .submit( toJson() )
                .map( (response, next) -> {
                    LOG.debug( "Status: " + response.status() + " (" + timer.elapsedHumanReadable() + ")" );
                    if (response.status() > 299) {
                        throw new IOException( "HTTP Status: " + response.status() );
                    }
                    String text = response.text();
                    var in = new BufferedReader( new StringReader( text ) );
                    Sequence.of( Exception.class, commands ).map( c -> (Command)c ).forEach( (command,i) -> {
                        command.parse( in );
                        if (i < commands.size() - 1) {
                            next.consumeResult( command );
                        }
                        else {
                            next.complete( command );
                        }
                    });
                });


//        var promise = new Promise.Completable<Command>();
//
//        Timer timer = Timer.start();
//        XMLHttpRequest request = XMLHttpRequest.create();
//        request.setOnReadyStateChange( () -> {
//            LOG.info( "Request ready state: " + request.getReadyState() );
//        });
//        request.open( "POST", "imap/", true );
//
//        request.onComplete( () -> {
//            try {
//                LOG.info( "Status: " + request.getStatus() + " (" + timer.elapsedHumanReadable() + ")" );
//                if (request.getStatus() > 299) {
//                    throw new IOException( "HTTP Status: " + request.getStatus() );
//                }
//                String response = request.getResponseText();
//                var in = new BufferedReader( new StringReader( response ) );
//                Sequence.of( Exception.class, commands ).map( c -> (Command)c ).forEach( (command,i) -> {
//                    command.parse( in );
//                    if (i < commands.size() - 1) {
//                        promise.consumeResult( command );
//                    }
//                    else {
//                        promise.complete( command );
//                    }
//                });
//            }
//            catch (Exception e) {
//                promise.completeWithError( e );
//            }
//        });
//        request.send( toJson() );
//        return promise;
    }


    /**
     *
     */
    public static class Command
            extends CommandData {

        /** Regex flags */
        public static final int     IGNORE_CASE = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;

        public static <E extends Exception> void matches( Pattern p, String s, Consumer<Matcher,E> handler ) throws E {
            var matcher = p.matcher( s );
            if (matcher.matches()) {
                handler.accept( matcher );
            }
        }

        private static volatile int tagCount = 0;

        protected String            tag = leftPad( Integer.toString( tagCount++ ), 4, '0' ) + ":";

        protected void parse( BufferedReader in ) throws Exception {
            while (parseLine(in.readLine())) { }
        }

        /**
         * @return False if this was the last line and parsing should stop.
         */
        protected boolean parseLine( String line ) {
            return !containsIgnoreCase( line, expected );
        }
    }


    public static class LoginCommand extends Command {

        public LoginCommand( String username, String passwd ) {
            // must not change for different request for servlet pool (no variable tag)
            command = String.format( "tag login %s %s", username, passwd );
            expected = "LOGIN completed";
        }
    }


    public static class LogoutCommand extends Command {

        public LogoutCommand() {
            command = String.format( "%s LOGOUT", tag );
            expected = "* BYE";
        }
    }

}
