/*
 * Copyright (C) 2024, the @authors. All rights reserved.
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
package areca.ui.viewer.model;

import java.util.Collection;
import java.util.Iterator;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public class Pojos<V>
    extends ModelBaseImpl
    implements ListModel<V> {

    private static final Log LOG = LogFactory.getLog( Pojos.class );

    protected Collection<V> coll;

    public Pojos( Collection<V> coll ) {
        this.coll = coll;
    }

    @Override
    public Iterator<V> iterator() {
        return coll.iterator();
    }

    @Override
    public int size() {
        return coll.size();
    }

    public boolean contains( Object o ) {
        return coll.contains( o );
    }

    public boolean add( V e ) {
        try {
            return coll.add( e );
        } finally {
            fireChangeEvent();
        }
    }

    public boolean remove( Object o ) {
        try {
            return coll.remove( o );
        } finally {
            fireChangeEvent();
        }
    }

    public void clear() {
        coll.clear();
        fireChangeEvent();
    }

}
