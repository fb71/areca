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

import java.lang.reflect.InvocationTargetException;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public abstract class MethodInfo
        implements Named, Annotated {

    private static final Log LOG = LogFactory.getLog( MethodInfo.class );

    protected String                   name;

    protected List<AnnotationInfo>     annotations;


    @Override
    public String toString() {
        return "MethodInfo[name=" + name + "]";
    }


    @Override
    public String name() {
        return name;
    }


    @Override
    public List<AnnotationInfo> annotations() {
        return annotations;
    }


    public abstract Object invoke( Object obj, Object... params ) throws InvocationTargetException;


    /**
     * {@link #invoke(Object, Object...)} this method and encapsulate the target
     * exceptions of any {@link InvocationTargetException} into a
     * {@link RuntimeException}.
     */
    public Object invokeThrowingRuntimeException( Object obj, Object... params ) {
        try {
            return invoke( obj, params );
        }
        catch (InvocationTargetException e) {
            LOG.warn( "invokeThrowingRuntimeException(): " + e.getTargetException(), e.getTargetException() );
            throw new RuntimeException( e.getTargetException() );
        }
    }

}
