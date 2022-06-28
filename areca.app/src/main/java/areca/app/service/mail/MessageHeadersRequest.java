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

import java.util.Date;
import java.util.Set;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

import org.apache.commons.lang3.Range;

import areca.app.service.matrix.OptString;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public class MessageHeadersRequest
        extends MailRequest<MessageHeadersRequest.MessageHeadersResponse> {

    private static final Log LOG = LogFactory.getLog( MessageHeadersRequest.class );

    public static final String  FILE_NAME = "message.headers";
    public static final String  MIN_NAME = "start";
    public static final String  MAX_NAME = "end";

    protected Range<Integer> msgNums;

    protected MessageHeadersRequest( RequestParams params, String folderName, Range<Integer> msgNums ) {
        super( params );
        this.msgNums = msgNums;
        setPath( folderName, FILE_NAME );
        if (msgNums != null) {
            addQuery( MIN_NAME, msgNums.getMinimum().toString() );
            addQuery( MAX_NAME, msgNums.getMaximum().toString() );
        }
    }

    /**
     *
     */
    public interface MessageHeadersResponse
            extends MailRequest.Response, JSObject {

        @JSProperty("messageHeaders")
        public MessageHeaders[] messageHeaders();

        public interface MessageHeaders extends JSObject {
            @JSProperty("messageId")
            public String messageId();

            @JSProperty("messageNum")
            public int messageNum();

            @JSProperty("subject")
            public OptString subject();

            @JSProperty("sentDate")
            public double _sentDate();

            public default Date sentDate() {
                return new Date( (long)_sentDate() );
            }

            @JSProperty("receivedDate")
            public double _receivedDate();

            public default Date receivedDate() {
                return new Date( (long)_receivedDate() );
            }

            @JSProperty("from")
            public EmailAddress[] from();

            @JSProperty("to")
            public EmailAddress[] to();

            @JSProperty("flags")
            public String[] _flags();

            public default Set<String> flags() {
                return Sequence.of( _flags() ).toSet();
            }
        }


        public interface EmailAddress extends JSObject {
            @JSProperty("type")
            public String type();

            @JSProperty("personal")
            public OptString personal();

            @JSProperty("address")
            public String address();
        }
    }

}
