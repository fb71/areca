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
package areca.app.service.carddav;

import static org.apache.commons.lang3.StringUtils.joinWith;
import static org.apache.commons.lang3.StringUtils.split;
import static org.apache.commons.lang3.StringUtils.splitPreserveAllTokens;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;

import java.util.ArrayList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Property;
import areca.ui.Property.ReadOnly;
import areca.ui.Property.ReadWrite;
import areca.ui.Property.ReadWrites;

/**
 * https://www.evenx.com/vcard-3-0-format-specification
 *
 * @author Falko Bräutigam
 */
public class VCard {

    private static final Log LOG = LogFactory.getLog( VCard.class );

    public static VCard parse( String text ) throws IOException {
        return new VCard().parse( new BufferedReader( new StringReader( text ) ) );
    }

    // instance *******************************************

    public ReadWrite<VCard,String>  fn = Property.create( this, "fn", (String)null );

    public ReadWrite<VCard,String>  lastname = Property.create( this, "lastname", (String)null );

    public ReadWrite<VCard,String>  firstname = Property.create( this, "firstname", (String)null );

    public ReadWrites<VCard,String> emails = Property.create( this, "email", new ArrayList<>() );

    public ReadOnly<VCard,String>   uid = Property.create( this, "uid", (String)null );

    public ReadOnly<VCard,String>   rev = Property.create( this, "rev", (String)null );


    protected VCard() {
    }


    @Override
    public String toString() {
        return String.format( "VCard[%s]", joinWith( ", ", fn, lastname, firstname, emails, uid, rev ) );
    }


    protected VCard parse( BufferedReader in ) throws IOException {
        in.readLine().equals( "BEGIN:VCARD" );
        in.readLine().equals( "VERSION:3.0" );

        var line = in.readLine();
        while (line != null && !line.equals( "END:VCARD" )) {
            // N:
            if (line.startsWith( "N:" )) {
                var n = splitPreserveAllTokens( line.substring( 2 ), ';' );
                lastname.set( n[0] );
                firstname.set( n[1] );
                //LASTNAME; FIRSTNAME; ADDITIONAL NAME; NAME PREFIX(Mr.,Mrs.); NAME SUFFIX)
            }
            // FN:
            else if (line.startsWith( "FN:" )) {
                fn.set( line.substring( 3 ) );
                LOG.debug( "FN: " + fn.get() );
            }
            // EMAIL:
            else if (line.startsWith( "EMAIL" )) {
                var parts = split( line.substring( 5 ), ';' );
                Sequence.of( parts ).forEach( part -> emails.add( substringAfterLast( part, ":" ) ) );
            }
            // UID:
            else if (line.startsWith( "UID:" )) {
                uid = Property.create( this, "uid", line.substring( 4 ) );
            }
            // REV:
            else if (line.startsWith( "REV:" )) {
                rev = Property.create( this, "rev", line.substring( 4 ) );
            }
            else {
                // LOG.warn( "Unhandled line: " + line );
            }
            line = in.readLine();
        }
        return this;
    }
}
