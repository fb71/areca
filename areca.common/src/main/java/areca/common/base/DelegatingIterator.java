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

/**
 *
 * @author Falko Bräutigam
 */
public class DelegatingIterator<S,T,E extends Exception>
        implements SequenceIterator<T,E> {

    protected SequenceIterator<S,E>     delegate;


    public DelegatingIterator( SequenceIterator<S,E> delegate ) {
        this.delegate = delegate;
    }

    public boolean hasNext() throws E {
        return delegate.hasNext();
    }

    @SuppressWarnings("unchecked")
    public T next() throws E {
        return (T)delegate.next();
    }

}
