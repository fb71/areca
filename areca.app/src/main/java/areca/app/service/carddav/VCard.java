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
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.apache.commons.lang3.StringUtils.substringBefore;

import java.util.ArrayList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import areca.common.Assert;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.Property;
import areca.ui.component2.Property.ReadOnly;
import areca.ui.component2.Property.ReadWrite;
import areca.ui.component2.Property.ReadWrites;

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

    public static class IMPP {

        public ReadWrite<IMPP,String>   name = Property.rw( this, "name", (String)null );

        public ReadWrite<IMPP,String>   type = Property.rw( this, "type", (String)null );
    }

    // instance *******************************************

    public ReadWrite<VCard,String>  fn = Property.rw( this, "fn", (String)null );

    public ReadWrite<VCard,String>  lastname = Property.rw( this, "lastname", (String)null );

    public ReadWrite<VCard,String>  firstname = Property.rw( this, "firstname", (String)null );

    public ReadWrites<VCard,String> emails = Property.rws( this, "email", new ArrayList<>() );

    public ReadWrites<VCard,String> phones = Property.rws( this, "phone", new ArrayList<>() );

    public ReadWrites<VCard,IMPP>   impp = Property.rws( this, "impp", new ArrayList<>() );

    public ReadWrite<VCard,String>  photo = Property.rw( this, "photo", (String)null );

    public ReadOnly<VCard,String>   uid = Property.rw( this, "uid", (String)null );

    public ReadOnly<VCard,String>   rev = Property.rw( this, "rev", (String)null );


    protected VCard() {
    }


    @Override
    public String toString() {
        return String.format( "VCard[%s]", joinWith( ", ", fn, lastname, firstname, emails, phones, uid, rev ) );
    }


    protected VCard parse( BufferedReader in ) throws IOException {
        Assert.that( in.readLine().equals( "BEGIN:VCARD" ) );
        Assert.that( in.readLine().equals( "VERSION:3.0" ) );

        var readline = in.readLine();
        while (readline != null && !readline.equals( "END:VCARD" )) {
            // concat one logical line
            var buf = new StringBuilder( 1024 ).append( readline );
            readline = in.readLine();
            while (readline.startsWith( " " ) || readline.startsWith( "\t" )) {
                buf.append( readline.substring( 1 ) );
                // preserve Base64 line breaks when PHOTO:
                if (buf.length() >= 5 && buf.substring( 0, 5 ).equalsIgnoreCase( "PHOTO" )) {
                    buf.append( "\n" );
                }
                readline = in.readLine();
            }
            var line = buf.toString();

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
                LOG.debug( "FN: " + fn.value() );
            }
            // EMAIL:
            else if (line.startsWith( "EMAIL" )) {
                var parts = split( line.substring( 5 ), ';' );
                Sequence.of( parts ).forEach( part -> emails.add( substringAfterLast( part, ":" ) ) );
            }
            // PHONE:
            else if (line.startsWith( "TEL" )) {
                var parts = split( line.substring( 3 ), ';' );
                Sequence.of( parts ).forEach( part -> phones.add( substringAfterLast( part, ":" ) ) );
            }
            // IMPP:
            else if (line.startsWith( "IMPP" )) {
                LOG.info( "IMPP: %s", line );
                var content = line.substring( 5 );
                impp.add( new IMPP()
                        .type.set( substringBefore( content, ":" ) )
                        .name.set( substringAfter( content, ":" ) ) );
            }
            // UID:
            else if (line.startsWith( "UID:" )) {
                uid = Property.rw( this, "uid", line.substring( 4 ) );
            }
            // REV:
            else if (line.startsWith( "REV:" )) {
                rev = Property.rw( this, "rev", line.substring( 4 ) );
            }
            // PHOTO;ENCODING=B;TYPE=JPEG:
            else if (line.startsWith( "PHOTO" )) {
                //var parts = split( substringBefore( line, ":" ), ';' );
                photo.set( substringAfterLast( line, ":" ) );
                LOG.debug( "PHOTO: %s", photo.value() );
            }
            else {
                // LOG.warn( "Unhandled line: " + line );
            }
        }
        return this;
    }
}
