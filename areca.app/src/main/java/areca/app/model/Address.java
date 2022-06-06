/*
 * Copyright (C) 2022, the @authors. All rights reserved.
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
package areca.app.model;

import java.util.regex.Pattern;

import areca.app.service.Service;
import areca.app.service.TransportService;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * Identifies a receipient a {@link TransportService} can use to send a message to.
 * An address is used in {@link Message} to identify From: and ReplyTo: fields.
 * A {@link Contact} usually hast multiple Addresses.
 * <p/>
 * An address can be encoded to and parsed from a {@link String} representation.
 *
 * @author Falko Bräutigam
 */
public class Address {

    private static final Log LOG = LogFactory.getLog( Address.class );

    public static final String  DELIM = "\\x00";
    public static final Pattern ENCODED = Pattern.compile("([^%s]+)%s([^%s]+)%s?([^%s]*)".replace( "%s", DELIM ) );

    /** {@link Service} specific prefix. */
    public String               prefix;

    /** The human readable, main part of the address. */
    public String               content;

    /** {@link Service} specific extension, or an empty String. */
    public String               ext = "";


    public String encoded() {
        return String.format( "%s\0%s\0%s", prefix, content, ext );
    }

    @Override
    public String toString() {
        return String.format( "[%s:%s]", prefix, content );
    }



    public static Address parseEncoded( String encoded ) {
        var matcher = ENCODED.matcher( encoded );
        if (!matcher.matches()) {
            throw new RuntimeException( "String is not a valid encoded Address: " + encoded );
        }
        return new Address() {{
            prefix = matcher.group( 1 );
            content = matcher.group( 2 );
            ext = matcher.group( 3 );
        }};
    }
}
