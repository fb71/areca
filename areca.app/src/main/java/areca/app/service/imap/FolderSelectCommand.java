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

import areca.app.service.imap.ImapRequest.Command;

/**
 *
 * @author Falko Bräutigam
 */
public class FolderSelectCommand extends Command {

    public FolderSelectCommand( String folderName ) {
        command = format( "%s SELECT %s", tag, folderName );
        expected = format( "%s OK", tag );
    }
}