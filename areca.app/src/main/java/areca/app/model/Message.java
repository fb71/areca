/*
 * Copyright (C) 2020, the @authors. All rights reserved.
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

import java.util.List;

import org.polymap.model2.DefaultValue;
import org.polymap.model2.Defaults;
import org.polymap.model2.Nullable;
import org.polymap.model2.Property;
import org.polymap.model2.Queryable;
import org.polymap.model2.query.Expressions;
import areca.app.service.Service;
import areca.app.service.TransportService;
import areca.common.Promise;
import areca.common.reflect.RuntimeInfo;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class Message
        extends Common {

    public static final MessageClassInfo info = MessageClassInfo.instance();

    public static final String      NO_ADDRESS = "";

    public static Message           TYPE;

    public enum ContentType {
        PLAIN, HTML /*,MARKDOWN*/
    }

    @Defaults
    public Property<Boolean>        outgoing;

    /**
     * An {@link Address} that identifies the origin sender of this Message.
     * It should identify the {@link Contact}, if one exists.
     */
    @Nullable
    @Queryable
    public Property<String>         fromAddress;

    /**
     * An {@link Address} that identifies the receipient this Message.
     */
    @Nullable
    @Queryable
    public Property<String>         toAddress;

    /**
     * An {@link Address} that is the {@link Service} specific path to reply to this
     * Message. In Matrix this specifies a "room". EMail may have a ReplyTo:.
     * {@link TransportService Sending} a message to this {@link Address} is the
     * Service specific and intented way to reply to a message.
     */
    @Nullable
    public Property<String>         replyAddress;

    @Queryable
    public Property<Long>           date;

    @Nullable
    @Defaults
    public Property<String>         content;

    @Nullable
    @DefaultValue( "PLAIN" )
    public Property<ContentType>    contentType;

    @Nullable
    public Property<String>         threadSubject;

    @Defaults
    public Property<Boolean>        unread;

    /**
     * A {@link Service} specific reference that is used to identify the origin of
     * this message in the backend store.
     */
    @Nullable
    @Queryable
    public Property<String>         storeRef;

    /**
     * Computed bidi association {@link Anchor#messages}.
     */
    public Promise<List<Anchor>> anchors() {
        return context.getUnitOfWork().query( Anchor.class )
                .where( Expressions.anyOf( Anchor.TYPE.messages, Expressions.id( id() ) ) )
                .executeCollect();
    }

}
