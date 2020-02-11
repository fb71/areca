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
package areca.rt.teavm.reflect;

import java.util.Optional;
import java.util.logging.Logger;

import java.lang.annotation.Annotation;

import org.teavm.metaprogramming.ReflectClass;
import org.teavm.metaprogramming.reflect.ReflectMethod;

import areca.common.reflect.MethodInfo;

/**
 *
 *
 * @author Falko Bräutigam
 */
public class TeaMethodDescriptor
        extends MethodInfo {

    private static final Logger LOG = Logger.getLogger( TeaMethodDescriptor.class.getName() );

    private ReflectMethod       m;

    private ReflectClass<?>     cl;


    public TeaMethodDescriptor( ReflectMethod m, ReflectClass cl ) {
        this.m = m;
        this.cl = cl;
    }


    @Override
    public String name() {
        return m.getName();
    }


    @Override
    public <R> Optional<R> annotation( Class<? extends Annotation> type ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

}
