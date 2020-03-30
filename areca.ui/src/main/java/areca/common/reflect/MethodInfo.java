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

import java.util.Optional;

import java.lang.annotation.Annotation;

/**
 *
 * @author Falko Bräutigam
 */
public class MethodInfo
        implements Named, Annotated {

    public String           name;

    @Override
    public String name() {
        return name;
    }

    @Override
    public <R extends AnnotationInfo> Optional<R> annotation( R type ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public <R extends Annotation> Optional<R> annotation( Class<R> type ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

    public void invoke( Object obj, Object... params ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

    //@Override
//    public <R extends AnnotationInfo> Optional<R> annotation( Class<R> type ) {
//        // XXX Auto-generated method stub
//        throw new RuntimeException( "not yet implemented." );
//    }

}
