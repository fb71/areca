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
package areca.common.reflect;

import java.util.List;
import java.util.Optional;

import java.lang.annotation.Annotation;

/**
 *
 * @author Falko Bräutigam
 */
public interface Annotated {

    public abstract List<AnnotationInfo> annotations();


    @SuppressWarnings("unchecked")
    public default <R extends AnnotationInfo> Optional<R> annotation( R type ) {
        for (AnnotationInfo a : annotations()) {
            if (type.annotationType().isAssignableFrom( a.annotationType() ) ) {
                return Optional.of( (R)a );
            }
        }
        return Optional.empty();

//        return annotations().stream()
//                .filter( a -> type.annotationType().isAssignableFrom( a.annotationType() ) )
//                .map( a -> (R)a )
//                .findAny();
    }


    public default <R extends Annotation> Optional<R> annotation( Class<R> type ) {
        throw new RuntimeException( "not yet implemented." );
    }

}
