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

import org.apache.commons.lang3.mutable.MutableInt;

import org.polymap.model2.Concerns;
import org.polymap.model2.Defaults;
import org.polymap.model2.ManyAssociation;
import org.polymap.model2.Nullable;
import org.polymap.model2.Property;
import org.polymap.model2.Queryable;
import areca.common.Promise;
import areca.common.reflect.RuntimeInfo;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class Anchor
        extends Common {

    public static final AnchorClassInfo info = AnchorClassInfo.instance();

    public static Anchor            TYPE;

    @Queryable
    public Property<String>         name;

    @Concerns(AnchorMessagesConcern.class)
    public ManyAssociation<Message> messages;

    @Defaults
    @Queryable
    public Property<Long>           lastMessageDate;

    @Nullable
    @Queryable
    public Property<String>         storeRef;


    public Promise<Integer> unreadMessagesCount() {
        // XXX a ComputedProperty would cache?!
        return messages.fetch()
                .reduce( new MutableInt(), (count,opt) -> opt.ifPresent( msg -> count.add( msg.unread.get() ? 1 : 0 ) ) )
                .map( count -> count.getValue() );
    }

}
