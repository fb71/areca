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
package areca.common.base;

import java.util.Iterator;

/**
 *
 * @author Falko Bräutigam
 */
public class DelegatingIterator<T,R>
        implements Iterator<R> {

    protected Iterator<T>       delegate;

    public DelegatingIterator( Iterator<T> delegate ) {
        this.delegate = delegate;
    }

    public boolean hasNext() {
        return delegate.hasNext();
    }

    public R next() {
        return (R)delegate.next();
    }

    public void remove() {
        delegate.remove();
    }

}
