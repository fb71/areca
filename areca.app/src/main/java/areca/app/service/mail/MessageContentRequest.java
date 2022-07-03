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

import java.util.Set;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

import areca.common.base.Opt;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public class MessageContentRequest
        extends MailRequest<MessageContentRequest.MessageContentResponse> {

    private static final Log LOG = LogFactory.getLog( MessageContentRequest.class );

    public static final String  FILE_NAME = "message.content";
    public static final String  NUM_NAME = "num";
    //public static final String  ID_NAME = "id";


    protected MessageContentRequest( RequestParams params, String folderName, Set<Integer> msgNums ) {
        super( params );
        setPath( folderName, FILE_NAME );
        if (msgNums != null) {
            for (var msgNum : msgNums) {
                addQuery( NUM_NAME, msgNum.toString() );
            }
        }
    }

    /**
     *
     */
    public interface MessageContentResponse
            extends MailRequest.Response, JSObject {

        @JSProperty("messageContent")
        public MessageContent[] messageContent();

        public interface MessageContent extends JSObject {
            @JSProperty("messageId")
            public String messageId();

            @JSProperty("messageNum")
            public int messageNum();

            @JSProperty("parts")
            public MessagePart[] parts();

            public default Opt<MessagePart> bodyParts( String... mimeTypes ) {
                for (var mimeType : mimeTypes) {
                    var found = Sequence.of( parts() ).first( part -> part.isType( mimeType ) ).orElse( null );
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
}
