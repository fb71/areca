/*
 * Copyright (C) 2021, the @authors. All rights reserved.
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

import java.util.NoSuchElementException;

import areca.common.base.Function.RFunction;
import areca.common.base.Predicate.RPredicate;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public class SequenceOpImpl<T,E extends Exception>
        extends Sequence<T,E> {

    private static final Log LOG = LogFactory.getLog( SequenceOpImpl.class );

    private static final Object EOS = new Object();
    private static final Object FILTERED = new Object();
    private static final Object UNKNOWN = new Object();

    /**
     * Creates a {@link Sequence} that produces a series of values in the given range.
     *
     * @param <R>
     * @param start The first value returned.
     * @param next The function that creates the next value.
     * @param as_long_as Continue the series as long as this condition is met.
     * @return Newly created {@link Sequence}.
     */
    public static <R> Sequence<R,RuntimeException> series( R start,
            RFunction<R,R> next,
            RPredicate<R> as_long_as ) {
        return new SequenceOpImpl<R,RuntimeException>( new Supplier<Object,RuntimeException>() {
            R elm = start;

            @Override
            public Object supply() throws RuntimeException {
                try {
                    return elm != EOS && as_long_as.test( elm ) ? elm : EOS;
                }
                finally {
                    elm = next.apply( elm );
                }
            }
        });
    }

    public static Sequence<Integer,RuntimeException> ofInts( int start, int end ) {
        return series( start, n -> n + 1, n -> n <= end);
    }


    // instance *******************************************

    private Supplier<?,E>   supplier;

    private Object[]        ops = new Object[10];

    private int             opsCount = 0;


    protected SequenceOpImpl( Supplier<?,E> supplier ) {
        super( null );
        this.supplier = supplier;
    }


    @Override
    public <R,RE extends E> Sequence<R,E> transform( Function<T,R,RE> function ) throws RE {
        ops[opsCount++] = function;
        return (Sequence<R,E>)this;
    }


    @Override
    public <R extends T, RE extends E> Sequence<R,E> filter( Predicate<T,RE> condition ) throws RE {
        ops[opsCount++] = condition;
        return (Sequence<R,E>)this;
    }


    protected boolean notFoundAndNotEOS( Object elm ) {
        return (elm == UNKNOWN || elm == EOS) && elm != EOS;
    }


    @Override
    @SuppressWarnings("unchecked")
    protected SequenceIterator<T,E> iterator() {
        return new SequenceIterator<>() {
            private Object next = UNKNOWN;

            @Override
            public boolean hasNext() throws E {
                while (notFoundAndNotEOS( next )) {
                    next = supplier.supply();
                    if (next != EOS) {
                        // ops
                        for (int i=0; i < opsCount; i++) {
                            if (ops[i] instanceof Function) {
                                next = ((Function<Object,Object,E>)ops[i]).apply( next );
                            }
                            else if (ops[i] instanceof Predicate) {
                                if (!((Predicate<Object,E>)ops[i]).test( next )) {
                                    next = UNKNOWN;
                                    break;
                                };
                            }
                            else {
                                throw new RuntimeException( "Unhandled op: " + ops[i] );
                            }
                        }
                    }
                }
                return next != EOS;
            }

            @Override
            public T next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                try {
                    return (T)next;
                }
                finally {
                    next = UNKNOWN;
                }
            }
        };
    }

}
