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

import java.util.regex.Pattern;

import areca.app.service.imap.ImapRequest.Command;

/**
 *
 * @author Falko Bräutigam
 */
public class FolderSelectCommand extends Command {

    public static final Pattern EXISTS = Pattern.compile( "\\* (\\d+) EXISTS", IGNORE_CASE );
    public static final Pattern RECENT = Pattern.compile( "\\* (\\d+) RECENT", IGNORE_CASE );

    public int      exists = -1;

    public int      recent = -1;

    public FolderSelectCommand( String folderName ) {
        command = format( "%s SELECT %s", tag, folderName );
        expected = format( "%s OK", tag );
    }

    @Override
    protected boolean parseLine( String line ) {
        if (super.parseLine( line )) {
            matches( EXISTS, line, matcher -> {
                exists = Integer.parseInt( matcher.group( 1 ) );
            });
            matches( RECENT, line, matcher -> {
                recent = Integer.parseInt( matcher.group( 1 ) );
            });
            return true;
        }
        else {
            return false;
        }
    }


}