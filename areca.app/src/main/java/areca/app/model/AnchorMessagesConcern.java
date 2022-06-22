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

import java.util.Collection;

import org.polymap.model2.ManyAssociation;
import org.polymap.model2.PropertyConcernBase;
import org.polymap.model2.query.Query;

import areca.common.Promise;
import areca.common.base.Opt;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class AnchorMessagesConcern
        extends PropertyConcernBase<Message>
        implements ManyAssociation<Message> {

    private static final Log LOG = LogFactory.getLog( AnchorMessagesConcern.class );

    public static final ClassInfo<AnchorMessagesConcern> info = AnchorMessagesConcernClassInfo.instance();


    @Override
    public ManyAssociation<Message> delegate() {
        return (ManyAssociation<Message>)super.delegate();
    }


    @Override
    public boolean add( Message msg ) {
        var anchor = context.<Anchor>getEntity();
        if (msg.date.get() > anchor.lastMessageDate.get()) {
            anchor.lastMessageDate.set( msg.date.get() );
        }
        return delegate().add( msg );
    }


    @Override
    public boolean addAll( Collection<? extends Message> c ) {
        throw new RuntimeException( "not yet implemented." );
    }


    @Override
    public Promise<Opt<Message>> fetch() {
        return delegate().fetch();
    }


    @Override
    public Query<Message> query() {
        return delegate().query();
    }

}
