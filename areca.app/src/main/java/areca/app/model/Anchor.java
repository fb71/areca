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

import org.polymap.model2.Concerns;
import org.polymap.model2.Defaults;
import org.polymap.model2.ManyAssociation;
import org.polymap.model2.Nullable;
import org.polymap.model2.Property;
import org.polymap.model2.Queryable;

import areca.common.Assert;
import areca.common.Promise;
import areca.common.base.Opt;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.NoRuntimeInfo;
import areca.common.reflect.RuntimeInfo;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class Anchor
        extends Common {

    private static final Log LOG = LogFactory.getLog( Anchor.class );

    public static final AnchorClassInfo info = AnchorClassInfo.instance();

    public static Anchor            TYPE;

    @Queryable
    public Property<String>         name;

    @Nullable
    public Property<String>         image;

    @Queryable
    @Concerns( AnchorMessagesConcern.class )
    public ManyAssociation<Message> messages;

    @Defaults
    @Queryable
    public Property<Long>           lastMessageDate;

    @Queryable
    @Defaults
    public Property<Integer>        unreadMessagesCount;

    /**
     * A {@link Service} specific reference that is used to identify the origin of
     * this message in the backend store.
     *
     * @see {@link #storeRef(Class)}
     * @see #setStoreRef(StoreRef)
     * @see StoreRef
     */
    @Queryable
    public Property<String>         storeRef;


    public void updateUnreadMessagesCount( int update ) {
        LOG.debug( "UnreadMessagesCount: %d %d", unreadMessagesCount.get(), update );
        unreadMessagesCount.set( unreadMessagesCount.get() + update );
    }

    @NoRuntimeInfo
    public <R extends StoreRef> Opt<R> storeRef( Class<R> type ) {
        return StoreRef.decode( type, storeRef.get() );
    }


    @NoRuntimeInfo
    public Anchor setStoreRef( StoreRef ref ) {
        this.storeRef.set( ref.encoded() );
        return this;
    }


    public Promise<?> delete( boolean deleteMessages ) {
        if (deleteMessages) {
            return messages.fetchCollect()
                    .then( msgs -> Promise.serial( msgs.size(), false, i -> msgs.get( i ).delete() ) )
                    .reduce2( 0, (result,next) -> next ? result++ : result )
                    .map( c -> {
                        LOG.info( "%s messages deleted.", c );
                        Assert.isEqual( 0, messages.size() );
                        context.getUnitOfWork().removeEntity( this );
                        return true;
                    });
        }
        else {
            // XXX dangling messages!?
            context.getUnitOfWork().removeEntity( this );
            return Promise.completed( null );
        }
    }

}
