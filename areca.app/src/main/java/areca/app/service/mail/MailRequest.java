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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.teavm.jso.json.JSON;

import areca.common.Platform;
import areca.common.Promise;
import areca.common.Timer;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public abstract class MailRequest<R extends MailRequest.Response> {

    private static final Log LOG = LogFactory.getLog( MailRequest.class );

    public static final String  ENCODING = "UTF-8";

    public static final String  SEP = "/";

    private String              path;

    private Map<String,Set<String>>   query = new HashMap<>();

    public RequestParams        params;


    protected MailRequest( RequestParams params ) {
        this.params = params;
    }


    protected void setPath( String... parts ) {
        path = Sequence.of( parts )
                .map( part -> encode( part ) )
                .reduce( (p1,p2) -> p1 + SEP + p2 )
                .orElse( "" );
    }


    protected boolean addQuery( String name, String value ) {
        return query.computeIfAbsent( name, __ -> new HashSet<>() ).add( value );
    }


    public Promise<R> submit() {
        var timer = Timer.start();
        var queryString = new StringBuilder( 256 );
        for (var entry : query.entrySet()) {
            for (var value : entry.getValue()) {
                queryString.append( queryString.length() == 0 ? "?" : "&" );
                queryString.append( entry.getKey() ).append( "=" ).append( encode( value ) );
            }
        }
        var xhr = Platform.xhr( "GET", "mail/" + path + queryString );

        for (var param : params.all) {
            if (param.value != null) {
                xhr.addHeader( param.headerName(), encode( param.value.toString() ) );
            }
        }
        return xhr.submit()
                .map( response -> {
                    LOG.debug( "Status: " + response.status() + " (" + timer.elapsedHumanReadable() + ")" );
                    if (response.status() > 299) {
                        throw new IOException( "HTTP Status: " + response.status() );
                    }
                    String text = response.text();
                    return parse( text );
                });
    }


    protected String encode( String s ) {
        try {
            return URLEncoder.encode( s, ENCODING );
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException( e );
        }
    }


    protected R parse( String response ) {
        return JSON.parse( response ).cast();
    }


    /**
     *
     */
    public interface Response {
    }
}
