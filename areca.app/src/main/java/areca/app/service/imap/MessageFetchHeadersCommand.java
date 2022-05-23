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
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;

import areca.app.service.imap.ImapRequest.Command;
import areca.common.Assert;
import areca.common.base.Opt;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public class MessageFetchHeadersCommand extends Command {

    private static final Log LOG = LogFactory.getLog( MessageFetchHeadersCommand.class );

    public static final Pattern FLAGS = Pattern.compile( "^ FLAGS \\(([^)]*)\\)", IGNORE_CASE );

    public static final Pattern HEADER = Pattern.compile( "\\* (\\d+) FETCH \\(BODY\\[HEADER.FIELDS \\(([^)]+)\\)\\] \\{(\\d+)\\}", IGNORE_CASE );

    public enum FieldEnum {
        SUBJECT, FROM, TO, DATE, MESSAGE_ID;

        @Override
        public String toString() {
            return super.toString().replace( "_", "-" ); // Message-ID
        }

        public boolean equalsString( String s ) {
            return toString().equalsIgnoreCase( s );
        }

        public static Opt<FieldEnum> valueOfString( String s ) {
            return Sequence.of( FieldEnum.values() ).filter( e -> e.equalsString( s ) ).first();
        }
    }

    public enum Flag {
        SEEN, ANSWERED, FLAGGED, DELETED, DRAFT, RECENT;

        public static Opt<Flag> valueOfString( String s ) {
            return Sequence.of( Flag.values() ).filter( e -> e.toString().equalsIgnoreCase( s ) ).first();
        }
    }


    // instance *******************************************

    /** Msg num -> {@link FieldEnum} + value */
    public Map<Integer,Map<FieldEnum,String>>   headers = new TreeMap<>();

    public Map<Integer,Set<Flag>>               flags = new TreeMap<>();

    private Integer                             currentMsgNum;

    private FieldEnum                           currentField;


    public MessageFetchHeadersCommand( Range<Integer> msgNum, FieldEnum field, FieldEnum... more ) {
        var fields = StringUtils.join( ArrayUtils.add( more, field ), " " );
        // var fields = field.toString() + Sequence.of( more ).reduce( "", (r,f) -> r + " " + f );
        command = format( "%s FETCH %d:%d (FLAGS BODY.PEEK[HEADER.FIELDS (%s)])", tag, msgNum.getMinimum(), msgNum.getMaximum(), fields );
        expected = format( "%s OK FETCH completed", tag );
    }


    @Override
    protected boolean parseLine( String line ) {
        if (super.parseLine( line )) {
            // delimiter (?)
            if (line.isBlank() || line.equals( ")" )) {
                return true;
            }
            // header
            var matcher = HEADER.matcher( line );
            var flagsMatcher = FLAGS.matcher( line );
            if (matcher.matches()) {
                currentMsgNum = Integer.valueOf( matcher.group( 1 ) );
                flags.put( currentMsgNum, new HashSet<>() );
                headers.put( currentMsgNum, new HashMap<>() );
            }
            // flags
            else if (flagsMatcher.find()) {
                LOG.info( "Flags: %s", line );
                Assert.notNull( currentMsgNum, "No current message: " + line );
                Sequence.of( flagsMatcher.group( 1 ).split( "[ ]+" ) )
                        .filter( s -> !s.isBlank() )
                        .map( s -> Flag.valueOfString( s.substring( 1 ) ) )
                        .filter( opt -> opt.isPresent() )
                        .forEach( flag -> flags.get( currentMsgNum ).add( flag.get() ) );
            }
            // content line continue
            else if (Character.isWhitespace( line.charAt( 0 ) )) {
                LOG.debug( "Content cont: '%s'", line );
                headers.get( currentMsgNum ).computeIfPresent( currentField, (__,content) ->
                       content + line.substring( 1 ) );
            }
            // content line start
            else if (currentMsgNum != null) {
                LOG.debug( "Content start: '%s'", line );
                currentField = FieldEnum.valueOfString( substringBefore( line, ": " ) )
                        .orElseThrow( () -> new RuntimeException( "Unknown header field: " + line ) );
                var content = substringAfter( line, ": " );

                //content = DecoderUtil.decodeEncodedWords( content, DecodeMonitor.STRICT );
                headers.get( currentMsgNum ).put( currentField, content );
            }
            return true;
        }
        else {
            return false;
        }
    }

}
