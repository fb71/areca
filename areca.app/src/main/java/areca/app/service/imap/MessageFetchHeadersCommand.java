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
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.apache.james.mime4j.util.MimeUtil;

import areca.app.service.imap.ImapRequest.Command;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public class MessageFetchHeadersCommand extends Command {

    private static final Log log = LogFactory.getLog( MessageFetchHeadersCommand.class );

    public static final Pattern PATTERN = Pattern.compile( "\\* (\\d+) FETCH \\(BODY\\[HEADER.FIELDS \\(([^)]+)\\)\\] \\{(\\d+)\\}", IGNORE_CASE );

    public enum FieldEnum {
        SUBJECT, FROM, TO, DATE, MESSAGE_ID;

        @Override
        public String toString() {
            return super.toString().replace( "_", "-" ); // Message-ID
        }

        public boolean equalsString( String s ) {
            return toString().equalsIgnoreCase( s );
        }

        public static FieldEnum valueOfString( String s ) {
            return Sequence.of( FieldEnum.values() ).filter( e -> e.equalsString( s ) ).single();
        }
    }


    // instance *******************************************

    public Map<Integer,Map<FieldEnum,String>>   headers = new TreeMap<>();

    private Integer                             currentMsgNum;


    public MessageFetchHeadersCommand( Range<Integer> msgNum, FieldEnum field, FieldEnum... more ) {
        var fields = StringUtils.join( ArrayUtils.add( more, field ), " " );
        // var fields = field.toString() + Sequence.of( more ).reduce( "", (r,f) -> r + " " + f );
        command = format( "%s FETCH %d:%d (BODY[HEADER.FIELDS (%s)])", tag, msgNum.getMinimum(), msgNum.getMaximum(), fields );
        expected = format( "%s OK FETCH completed", tag );
    }


    @Override
    protected boolean parseLine( String line ) {
        if (super.parseLine( line )) {
            // delimiter (?)
            if (line.equals( "" ) || line.equals( ")" )) {
                return true;
            }
            // header
            var matcher = PATTERN.matcher( line );
            if (matcher.matches()) {
                currentMsgNum = Integer.valueOf( matcher.group( 1 ) );
                return true;
            }
            // content line
            log.info( "Content line: '%s'", line );
            var field = FieldEnum.valueOfString( substringBefore( line, ": " ) );
            var content = substringAfter( line, ": " );
            content = MimeUtil.unscrambleHeaderValue( content );
            headers.computeIfAbsent( currentMsgNum, k -> new HashMap<>() ).put( field, content );
            return true;
        }
        else {
            return false;
        }
    }

}
