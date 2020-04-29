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
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 *
 * @author Falko Bräutigam
 */
public class Sequence<T, E extends Exception> {

    @SafeVarargs
    public static <R> Sequence<R,RuntimeException> of( R... elements ) {
        return of( Arrays.asList( elements ) );
    }

    public static <R> Sequence<R,RuntimeException> of( Iterable<R> elements ) {
        return of( RuntimeException.class, elements );
    }

    public static <R,E extends Exception> Sequence<R,E> of( Class<E> type, Iterable<R> elements ) {
        return new Sequence<R,E>( () -> {
            Iterator<R> it = elements.iterator();
            return () -> it.hasNext() ? it.next() : null;
        });
    }


    // instance *******************************************

    protected Supplier<SequenceIterator<T,E>> iterate;


    protected Sequence( Supplier<SequenceIterator<T,E>> iterate ) {
        this.iterate = iterate;
    }


    public <R,RE extends E> Sequence<R,E> transform( Function<T,R,RE> function ) throws RE {
        SequenceIterator<T,E> delegate = iterate.get();
        return new Sequence<R,E>( () -> {
            return () -> function.apply( delegate.next() );
        });
    }


    /**
     * See {@link Stream#forEach(java.util.function.Consumer)}
     */
    public <RE extends E> void forEach( Consumer<T,RE> action ) throws E {
        SequenceIterator<T,E> it = iterate.get();
        for (T elm = it.next(); elm != null; elm = it.next()) {
            action.accept( elm );
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
        SequenceIterator<T,E> it = iterate.get();
        for (T elm = it.next(); elm != null; elm = it.next()) {
            @SuppressWarnings("unchecked")
            R r = result != null
                    ? accumulator.apply( result, elm )
                    : (R)elm;  // in case no identity was given
            result = combiner.apply( result, r );
        }
        return result;
    }


    public <A,R> R collect( Collector<T,A,R> collector ) throws E {
        A result = collector.supplier().get();
        BiConsumer<A,T> accumulator = collector.accumulator();
        forEach( elm -> accumulator.accept( result, elm ) );
        return collector.finisher().apply( result );
    }


//    public <RE extends E> List<T> asList() throws E {
//        for (Iterator<T> it=iterate(); it.hasNext(); ) {
//            action.accept( it.next() );
//        }
//    }

}
