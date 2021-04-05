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
import java.util.regex.Pattern;

import areca.app.service.imap.ImapRequest.Command;

/**
 *
 *
 * @author Falko Bräutigam
 */
public class FolderListCommand
        extends Command {

    public static final Pattern PATTERN = Pattern.compile( "\\* LIST \\([^)]*\\) \"/\" \"([^\"]*)\"", IGNORE_CASE );

    public List<String>         folderNames = new ArrayList<>( 128 );

    public FolderListCommand() {
        command = String.format( "%s LIST \"\" \"*\"", tag );
        expected = String.format( "%s OK", tag );
    }

    @Override
    protected boolean parseLine( String line ) {
        if (super.parseLine( line )) {
            var matcher = PATTERN.matcher( line );
            if (!matcher.matches()) {
                throw new RuntimeException("Line does not match: '" + line + "'" );
            }
            folderNames.add( matcher.group( 1 ) );
            return true;
        }
        return false;
    }
}