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
package areca.app.service.mail;

import java.util.regex.Pattern;

import areca.app.model.Address;
import areca.common.Assert;
import areca.common.base.Opt;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public class EmailAddress
        extends Address {

    private static final Log LOG = LogFactory.getLog( EmailAddress.class );

    public static final String  PREFIX = "email";

    public static Pattern       EMAIL_EXT = Pattern.compile( "([^<]+)[ ]*<([^>]+)>" );

    public static Pattern       NAME_COMMA = Pattern.compile( "([^,]+),[ ]*(.+)" );

    public static Pattern       NAME_SIMPLE = Pattern.compile( "([^ ]+)[ ]*(.*)" );

    public static Pattern       NAME_DOT = Pattern.compile( "([^.]+)[.](.+)" );

    public static Pattern       EMAIL = Pattern.compile( "([^@]+)@(.*)" );


    public static Opt<EmailAddress> check( Address check ) {
        Assert.that( !EmailAddress.class.isInstance( check ) );
        return Opt.of( PREFIX.equals( check.prefix )
                ? new EmailAddress( check.content )
                : null );
    }

    // instance *******************************************

    public AddressParts         parts;

    public EmailAddress( String email ) {
        this.prefix = PREFIX;
        this.parts = addressParts( email );
        this.content = parts.pure;
    }


    public AddressParts addressParts( String email ) {
        // extended
        var extMatch = EMAIL_EXT.matcher( email );
        if (extMatch.matches()) {
            String name = extMatch.group( 1 ).replace( "\"", "" ).trim();
            // comma separated name
            var commaMatch = NAME_COMMA.matcher( name );
            if (commaMatch.matches()) {
                return new AddressParts() {{first = commaMatch.group( 2 ); last = commaMatch.group( 1 ); pure = extMatch.group( 2 );}};
            }
            // normal name
            var normalMatch = NAME_SIMPLE.matcher( name );
            if (normalMatch.matches()) {
                return new AddressParts() {{first = normalMatch.group( 1 ); last = normalMatch.group( 2 ); pure = extMatch.group( 2 );}};
            }
            throw new RuntimeException( "No name match: " + name );
        }
        // just an email address
        var emailMatch = EMAIL.matcher( email );
        if (emailMatch.matches()) {
            var name = emailMatch.group( 1 );
            var dorMatch = NAME_DOT.matcher( name );
            if (dorMatch.matches()) {
                return new AddressParts() {{first = dorMatch.group( 1 ); last = dorMatch.group( 2 ); pure = email;}};
            }
            else {
                return new AddressParts() {{first = name; last = ""; pure = email;}};
            }
        }
        //
        return new AddressParts() {{pure = email;}};
        //throw new RuntimeException( "No email match: " + email );
    }


    public static class AddressParts {
        String first;
        String last;
        String pure;
    }

}
