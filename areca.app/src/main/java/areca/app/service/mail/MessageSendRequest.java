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

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.json.JSON;

import areca.common.base.Opt;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public class MessageSendRequest
        extends MailRequest<MessageSendRequest.MessageSendResponse> {

    private static final Log LOG = LogFactory.getLog( MessageSendRequest.class );

    public static final String  FILE_NAME = "message.send";

    public MessageSendRequest( RequestParams params, Message msg ) {
        super( params );
        setPath( FILE_NAME );
        this.content = JSON.stringify( msg );
    }

    /**
     *
     */
    public interface MessageSendResponse
            extends MailRequest.Response, JSObject {

        @JSProperty( "count" )
        public int count();

        @JSProperty( "messageId" )
        public String messageId();
    }


    /**
     *
     */
    public static abstract class Message implements JSObject {

        @JSBody( script = "return {};" )
        public static native Message create();

        @JSProperty("subject")
        public abstract void setSubject( String value );

        @JSProperty("from")
        public abstract void setFrom( String value );

        @JSProperty("to")
        public abstract void setTo( String value );

        @JSProperty("text")
        public abstract void setText( String value );

        /**
         * This is recognized for {@link MessageAppendRequest}.
         * Do not set this for sending.
         */
        @JSProperty("messageId")
        public abstract void setMessageId( String messageId );

        @JSProperty("parts")
        public abstract MessagePart[] parts();

        public Opt<MessagePart> bodyParts( String... mimeTypes ) {
            for (var mimeType : mimeTypes) {
                var found = Sequence.of( parts() ).first( part -> part.isType( mimeType ) ).orNull();
                if (found != null) {
                    return Opt.of( found );
                }
            }
            return Opt.absent();
        }
    }


    public interface MessagePart extends JSObject {
        @JSProperty("type")
        public String type();

        public default boolean isType( String type ) {
            return type().toLowerCase().contains( type.toLowerCase() );
        }

        @JSProperty("content")
        public String content();
    }
}
