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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 *
 * @author Falko Bräutigam
 */
public class RequestParams {

    public static final String  REQUEST_HEADER_PREFIX = "X-imap-";

    public List<Param>       all = new ArrayList<>();

    public Param            username = new Param( "username", null );

    public Param            password = new Param( "password", null );

    public Param            host = new Param( "host", null );

    public Param            port = new Param( "port", null );

//    props.put( "mail.imap.ssl.enable", "true");
//    props.put( "mail.imap.ssl.trust", "mail.polymap.de");


    public Optional<Param> fromRequestHeader( String header ) {
        for (var param : all) {
            if (header.equals( param.headerName() )) {
                return Optional.of( param );
            }
        }
        return Optional.empty();
    }


    @Override
    public String toString() {
        var result = new StringBuilder( 256 ).append( "RequestParams[" );
        for (var param : all) {
            result.append( param.name ).append( "=" )
                    .append( !param.name.equals( "password" ) ? param.value : "???" ).append( ", " );
        }
        return result.append( "]" ).toString();
    }


    @Override
    public int hashCode() {
        int result = 1;
        for (var param : all) {
            result = 31 * result + (param.value == null ? 0 : param.value.hashCode());
        }
        return result;
    }


    @Override
    public boolean equals( Object obj ) {
        if (this == obj) {
            return true;
        }
        else if (obj instanceof RequestParams) {
            for (int i = 0; i < all.size(); i++) {
                if (!Objects.equals( all.get( i ).value, ((RequestParams)obj).all.get( i ).value)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }


    /**
     *
     */
    protected class Param {
        public String   name;
        public String   value;

        public Param( String name, String defaultValue ) {
            this.name = name;
            this.value = defaultValue;
            RequestParams.this.all.add( this );
        }

        public String headerName() {
            return REQUEST_HEADER_PREFIX + name;
        }
    }

}
