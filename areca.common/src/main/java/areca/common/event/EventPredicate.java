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
package areca.common.event;

import java.util.EventObject;

import areca.common.base.Predicate;

/**
 *
 * @author Falko Bräutigam
 */
public interface EventPredicate
         extends Predicate<EventObject,RuntimeException> {

    /**
     * Evaluates this predicate on the given argument.
     *
     * @param t the input argument
     * @return {@code true} if the input argument matches the predicate,
     * otherwise {@code false}
     */
    @Override
    boolean test( EventObject  t );

    @Override
    default EventPredicate and( Predicate<? super EventObject,RuntimeException> other ) {
        return (EventPredicate)Predicate.super.and( other );
    }

    @Override
    default Predicate<EventObject,RuntimeException> negate() {
        return (EventPredicate)Predicate.super.negate();
    }

    @Override
    default Predicate<EventObject,RuntimeException> or( Predicate<? super EventObject,RuntimeException> other ) {
        return (EventPredicate)Predicate.super.or( other );
    }

}
