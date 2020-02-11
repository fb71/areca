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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.teavm.metaprogramming.CompileTime;
import org.teavm.metaprogramming.Meta;
import org.teavm.metaprogramming.Metaprogramming;
import org.teavm.metaprogramming.ReflectClass;
import org.teavm.metaprogramming.Value;
import org.teavm.metaprogramming.reflect.ReflectMethod;

import areca.common.reflect.MethodInfo;
import areca.common.reflect.ReflectionSupport;

/**
 *
 * @author Falko Bräutigam
 */
@CompileTime
public class TvmReflectionSupport
        extends ReflectionSupport {

    private static final Logger LOG = Logger.getLogger( TvmReflectionSupport.class.getName() );

    public static void init() {
        instance = new TvmReflectionSupport();
    }


    @Meta
    private static native Map<String,MethodInfo> _methodsOf( Class<?> cl, Object obj );


    private static void _methodsOf( ReflectClass<Object> cl, Value<Object> obj ) {
        System.out.println( "META! ---- " + cl );
        Map<String, MethodInfo> result = new HashMap<>();
        for (ReflectMethod m : cl.getMethods()) {
            System.out.println( "    " + m.getName() );
            result.put( m.getName(), new TeaMethodDescriptor( m, cl ) );
        }
        Metaprogramming.exit(() -> {
            return result;
        });
    }


    @Override
    public Map<String,MethodInfo> methodsOf( Class<?> cl ) {
        return _methodsOf( cl, null );
    }

}
