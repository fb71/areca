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

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.mutable.MutableInt;

import areca.common.Assert;

/**
 *
 * @param <T> The type of the elements of this sequence.
 * @param <E> The super-type of any exception that might be thrown during processing.
 * @author Falko Bräutigam
 */
public abstract class Sequence<T, E extends Exception> {

//    /**
//     * A {@link Sequence} that does not throw any checked {@link Exception} during
//     * processing.
//     */
//    public static abstract class Seq<T> extends Sequence<T,RuntimeException> {
//        protected Seq( Sequence<?,RuntimeException> parent ) {
//            super( parent );
//        }
//    }

    @SafeVarargs
    public static <R> Sequence<R,RuntimeException> of( R... elements ) {
        return of( Arrays.asList( elements ) );
    }


    public static <R> Sequence<R,RuntimeException> of( Iterable<R> elements ) {
        return of( elements, RuntimeException.class );
    }


    @SafeVarargs
    public static <R,E extends Exception> Sequence<R,E> of( Class<E> type, R... elements ) {
        return of( Arrays.asList( elements ), type );
    }


    public static <R,E extends Exception> Sequence<R,E> of( Iterable<R> elements, Class<E> type ) {
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


    public static <R,E extends Exception> Sequence<R,E> of( Class<E> type, Iterator<R> iterator ) {
        return new Sequence<R,E>( null ) {
            int callCount = 0;
            @Override
            protected SequenceIterator<R,E> iterator() {
                Assert.that( callCount++ == 0, "Sequence.of(Iterator) must not be iterated multiple times." );
                return new DelegatingIterator<R,R,E>( null ) {
                    @Override public R next() throws E {
                        return iterator.next();
                    }
                    @Override public boolean hasNext() throws E {
                        return iterator.hasNext();
                    }
                };
            }
        };
    }


    // instance *******************************************

    protected Sequence<?,E>     parent;


    protected Sequence( Sequence<?,E> parent ) {
        this.parent = parent;
    }


    @SuppressWarnings("unchecked")
    protected <R> Sequence<R,E> parent() {
        return (Sequence<R,E>)parent;
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


    /**
     * Same as {@link #transform(Function)}
     */
    public <R,RE extends E> Sequence<R,E> map( Function<T,R,RE> function ) throws RE {
        return transform( function );
    }


    public <RE extends E> Sequence<T,E> filter( Predicate<T,RE> condition ) throws RE {
        return new Sequence<T,E>( this ) {
            @Override
            @SuppressWarnings("unchecked")
            protected SequenceIterator<T,E> iterator() {
                return new DelegatingIterator<T,T,E>( (SequenceIterator<T,E>)parent.iterator() ) {
                    private T nextElm = null;

                    @Override
                    public boolean hasNext() throws E {
                        if (nextElm != null) {
                            return true;
                        }
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
                    @Override
                    public T next() throws E {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }
                        T result = Assert.notNull( nextElm );
                        nextElm = null;
                        return result;
                    }
                };
            }
        };
    }


    /**
     * Returns the first element, if at least one element is present in this
     * {@link Sequence}.
     */
    public Opt<T> first() throws E {
        SequenceIterator<T,E> it = iterator();
        return it.hasNext() ? Opt.of( it.next() ) : Opt.absent();
    }


    /**
     * Returns the one and only element in this {@link Sequence}.
     *
     * @throws NoSuchElementException If the sequence contains no element.
     * @throws IllegalStateException If the sequence contains more than one element.
     */
    public T single() throws E {
        SequenceIterator<T,E> it = iterator();
        try {
            if (!it.hasNext()) {
                throw new NoSuchElementException( "Sequence contains no element." );
            }
            return it.next();
        }
        finally {
            if (it.hasNext()) {
                throw new IllegalStateException( "Sequence contains more than one element." );
            }
        }
    }


    public <RE extends E> Opt<T> first( Predicate<T,RE> condition ) throws E {
        return filter( condition ).first();
    }



    public int count() throws E {
        return reduce( new MutableInt( 0 ), (result,elm) -> {result.increment(); return result;} ).intValue();
    }


    public Sequence<T,E> concat( Sequence<T,E> other ) {
        return new Sequence<T,E>( this ) {
            @Override
            @SuppressWarnings("unchecked")
            protected SequenceIterator<T,E> iterator() {
                return new DelegatingIterator<T,T,E>( null ) {
                    Iterator<SequenceIterator<T,E>> outer = Arrays.asList(((Sequence<T,E>)parent).iterator(), other.iterator() ).iterator();
                    SequenceIterator<T,E>           inner;
                    int                             index;
                    @Override
                    public boolean hasNext() throws E {
                        while (inner == null || !inner.hasNext()) {
                            if (!outer.hasNext()) {
                                return false;
                            }
                            inner = outer.next();
                        }
                        return inner.hasNext();
                    }
                    @Override
                    public T next() throws E {
                        if (!hasNext()) {
                            throw new NoSuchElementException( "index=" + index );
                        }
                        index ++;
                        return inner.next();
                    }
                };
            }
        };
    }


    public <RE extends E> Sequence<T,E> onEach( Consumer<T,RE> consumer ) throws RE {
        return new Sequence<T,E>( this ) {
            @Override
            @SuppressWarnings("unchecked")
            protected SequenceIterator<T,E> iterator() {
                return new DelegatingIterator<T,T,E>( (SequenceIterator<T,E>)parent.iterator() ) {
                    @Override
                    public T next() throws E {
                        T next = delegate.next();
                        consumer.accept( next );
                        return next;
                    }
                };
            }
        };
    }


    /**
     * Performs the given operation for each element of this {@link Sequence}.
     *
     * @see Stream#forEach(java.util.function.Consumer)
     */
    public <RE extends E> void forEach( Consumer<T,RE> consumer ) throws E {
        for (SequenceIterator<T,E> it = iterator(); it.hasNext(); ) {
            consumer.accept( it.next() );
        }
    }


    /**
     * Performs the given operation for each element of this {@link Sequence}. The
     * operation receives the <b>index</b> of the current element.
     *
     * @see Stream#forEach(java.util.function.Consumer)
     * @param <RE>
     * @param operation
     * @throws E
     */
    public <RE extends E> void forEach( BiConsumer<T,Integer,RE> operation ) throws E {
        int i = 0;
        for (SequenceIterator<T,E> it=iterator(); it.hasNext(); i++) {
            operation.accept( it.next(), i );
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
        java.util.function.BiConsumer<A,T> accumulator = collector.accumulator();
        forEach( elm -> accumulator.accept( result, elm ) );
        return collector.finisher().apply( result );
    }


    /**
     *
     */
    public <R extends Collection<T>> R collect( Supplier<R,RuntimeException> supplier ) throws E {
        return reduce( supplier.get(), (r, elm) -> {r.add( elm ); return r;} );
    }


    public <R> R[] toArray( Function<Integer,R[],RuntimeException> generator ) throws E {
        ArrayList<T> l = collect( Collectors.toCollection( ArrayList::new ) );
        return l.toArray( generator.apply( l.size() ) );
    }


    /**
     * Collects the elements into an {@link ArrayList}.
     *
     * @return Newly created {@link ArrayList}.
     * @throws E
     */
    public ArrayList<T> toList() throws E {
        return collect( Collectors.toCollection( ArrayList::new ) );
    }


    /**
     *
     *
     * @param <K>
     * @param <RE>
     * @param keyMapper
     * @return Newly created {@link HashMap}.
     * @throws E
     * @throws {@link IllegalStateException} If a key is not unique.
     */
    public <K,RE extends E> HashMap<K,T> toMap( Function<T,K,RE> keyMapper ) throws E {
//        return reduce( new HashMap<K,T>(), (r, elm) -> {
//            if (r.put( keyMapper.apply( elm ), elm ) != null) {
//                throw new IllegalStateException( "Key already exists: " + keyMapper.apply( elm ) );
//            }
//            return r;
//        });

        HashMap<K,T> result = new HashMap<>();
        forEach( elm -> {
            if (result.put( keyMapper.apply( elm ), elm ) != null) {
                throw new IllegalStateException( "Key already exists: " + keyMapper.apply( elm ) );
            }
        });
        return result;
    }


    /**
     * Returns a {@link Collection} view of this {@link Sequence}. In contrast to
     * collect methods this does not copy the elements into a newly created
     * Collection. The resulting Collection reflects changes of the underlying
     * Sequence.
     * <p>
     * The {@link Collection#size()} method <b>caches</b> the result first time it is
     * called!
     *
     * @param <RE>
     * @return An unmodifiable {@link Collection} view of this {@link Sequence}.
     * @throws E
     */
    public <RE extends E> Collection<T> asCollection() throws E {
        return new AbstractCollection<T>() {
            /** fail fast if Sequence throws checked Exception */
            @SuppressWarnings("unchecked")
            Sequence<T,RuntimeException>    noExceptionSequence = (Sequence<T,RuntimeException>)Sequence.this;
            Lazy<Integer,RuntimeException>  size = new Lazy<>( () -> noExceptionSequence.count() );
            @Override
            public Iterator<T> iterator() {
                return noExceptionSequence.asIterable().iterator();
            }
            @Override
            public int size() {
                return size.get();
            }
        };
    }


    /**
     * Returns an {@link Iterable} view of this {@link Sequence}. In contrast to
     * collect methods this does not copy the elements into a newly created
     * Collection. If the backing {@link Sequence} changes then the resulting
     * {@link Iterable} will reflect these changes.
     *
     * @param <RE>
     * @throws E
     */
    public <RE extends E> Iterable<T> asIterable() throws E {
        return new Iterable<T>() {
            /** fail fast toIterator() method if Sequence throws checked Exception */
            @SuppressWarnings("unchecked")
            Sequence<T,RuntimeException> noExceptionSequence = (Sequence<T,RuntimeException>)Sequence.this;

            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    SequenceIterator<T,RuntimeException> delegate = noExceptionSequence.iterator();
                    @Override
                    public boolean hasNext() {
                        return delegate.hasNext();
                    }
                    @Override
                    public T next() {
                        return delegate.next();
                    }
                };
            }
        };
    }


    @Override
    public String toString() {
        try {
            return asCollection().toString();
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException( e );
        }
    }

}
