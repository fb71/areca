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

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.apache.commons.lang3.mutable.MutableInt;

/**
 *
 * @author Falko Bräutigam
 */
public abstract class Sequence<T, E extends Exception> {

    @SafeVarargs
    public static <R> Sequence<R,RuntimeException> of( R... elements ) {
        return of( Arrays.asList( elements ) );
    }


    public static <R> Sequence<R,RuntimeException> of( Iterable<R> elements ) {
        return of( RuntimeException.class, elements );
    }


    public static <R,E extends Exception> Sequence<R,E> of( Class<E> type, Iterable<R> elements ) {
        return new Sequence<R,E>( null ) {
            @Override
            protected SequenceIterator<R,E> iterator() {
                return new DelegatingIterator<R,R,E>( null ) {
                    protected Iterator<R> it = elements.iterator();
                    @Override public R next() throws E {
                        return it.next();
                    }
                    @Override public boolean hasNext() throws E {
                        return it.hasNext();
                    }
                };
            }
        };
    }


    // instance *******************************************

    protected Sequence<?,E>     parent;


    protected Sequence( Sequence<?,E> parent) {
        this.parent = parent;
    }

    protected abstract SequenceIterator<T,E> iterator();



    public <R,RE extends E> Sequence<R,E> transform( Function<T,R,RE> function ) throws RE {
        return new Sequence<R,E>( this ) {
            @Override
            @SuppressWarnings("unchecked")
            protected SequenceIterator<R,E> iterator() {
                return new DelegatingIterator<T,R,E>( (SequenceIterator<T,E>)parent.iterator() ) {
                    @Override
                    public R next() throws E {
                        return function.apply( delegate.next() );
                    }
                };
            }
        };
    }


    public <RE extends E> Sequence<T,E> filter( Predicate<T,RE> condition ) throws RE {
        return new Sequence<T,E>( this ) {
            @Override
            @SuppressWarnings("unchecked")
            protected SequenceIterator<T,E> iterator() {
                return new DelegatingIterator<T,T,E>( (SequenceIterator<T,E>)parent.iterator() ) {
                    private T nextElm = null;
                    @Override
                    public T next() throws E {
                        if (nextElm == null) {
                            throw new NoSuchElementException();
                        }
                        return nextElm;
                    }
                    @Override
                    public boolean hasNext() throws E {
                        nextElm = null;
                        while (delegate.hasNext()) {
                            T candidate = delegate.next();
                            if (condition.test( candidate )) {
                                nextElm = candidate;
                                return true;
                            }
                        }
                        return false;
                    }
                };
            }
        };
    }


    /**
     * See {@link Stream#forEach(java.util.function.Consumer)}
     */
    public <RE extends E> void forEach( Consumer<T,RE> action ) throws E {
        for (SequenceIterator<T,E> it=iterator(); it.hasNext();) {
            action.accept( it.next() );
        }
    }


    /**
     * See {@link Stream#reduce(BinaryOperator)}
     */
    public final T reduce( BinaryOperator<T> accumulator ) throws E {
        return reduce( null, accumulator, (result,r) -> r );
    }


    public final <R> R reduce( R identity, BiFunction<R, ? super T, R> accumulator ) throws E {
        return reduce( identity, accumulator, (result,r) -> r );
    }


    /**
     * See {@link Stream#reduce(Object, BiFunction, BinaryOperator)}
     */
    public final <R> R reduce( R identity, BiFunction<R, ? super T, R> accumulator, BinaryOperator<R> combiner ) throws E {
        R result = identity;
        for (SequenceIterator<T,E> it=iterator(); it.hasNext();) {
            T elm = it.next();
            @SuppressWarnings("unchecked")
            R r = result != null
                    ? accumulator.apply( result, elm )
                    : (R)elm;  // in case no identity was given
            result = combiner.apply( result, r );
        }
        return result;
    }


    /**
     * See {@link Stream#collect(Collector)}
     */
    public <A,R> R collect( Collector<T,A,R> collector ) throws E {
        A result = collector.supplier().get();
        BiConsumer<A,T> accumulator = collector.accumulator();
        forEach( elm -> accumulator.accept( result, elm ) );
        return collector.finisher().apply( result );
    }


    public int count() throws E {
        return reduce( new MutableInt( 0 ), (result,elm) -> {result.increment(); return result;} ).intValue();
    }


//    public <RE extends E> List<T> asList() throws E {
//        for (Iterator<T> it=iterate(); it.hasNext(); ) {
//            action.accept( it.next() );
//        }
//    }

}
