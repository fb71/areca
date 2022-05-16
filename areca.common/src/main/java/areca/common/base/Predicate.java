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

import areca.common.Assert;

/**
 * Similar to {@link java.util.function.Predicate} but allows checked Exceptions.
 *
 * @author Falko Bräutigam
 */
public interface Predicate<T,E extends Exception> {

    public interface RPredicate<TT>
            extends Predicate<TT,RuntimeException> {

//        public default RPredicate<TT> and( RPredicate<? super TT> other ) {
//            return t -> Predicate.super.and( other ).test( t );
//        }
    }

    /**
     * Evaluates this predicate on the given argument.
     */
    public boolean test(T t);

    /**
     * See {@link java.util.function.Predicate#and(java.util.function.Predicate)}
     */
    public default Predicate<T,E> and( Predicate<? super T,E> other ) {
        Assert.notNull( other );
        return (t) -> test(t) && other.test(t);
    }

    /**
     * See {@link java.util.function.Predicate#negate()}
     */
    public default Predicate<T,E> negate() {
        return (t) -> !test(t);
    }

    /**
     * See {@link java.util.function.Predicate#or(java.util.function.Predicate)}
     */
    public default Predicate<T,E> or( Predicate<? super T,E> other ) {
        Assert.notNull( other );
        return (t) -> test(t) || other.test(t);
    }

}
