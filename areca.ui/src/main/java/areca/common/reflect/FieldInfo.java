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

import java.lang.reflect.Field;

/**
 * {@link Field}
 * @author Falko Bräutigam
 */
public abstract class FieldInfo
        implements Named, Annotated, Typed {

    protected Class<?>              type;

    protected GenericType           genericType;

    protected String                name;

    protected List<AnnotationInfo>  annotations;

    protected ClassInfo<?>          declaringClassInfo;


    @Override
    public String toString() {
        return "FieldInfo[name=" + name + "]";
    }

    @Override
    public Class<?> type() {
        return type;
    }

    public GenericType genericType() {
        return genericType;
    }

    @Override
    public String name() {
        return name;
    }

    public ClassInfo<?> declaringClassInfo() {
        return declaringClassInfo;
    }

    public Class<?> declaringClass() {
        return declaringClassInfo.type();
    }

    @Override
    public List<AnnotationInfo> annotations() {
        return annotations;
    }

    public abstract Object get( Object target ) throws IllegalArgumentException;

    public abstract void set( Object target, Object value ) throws IllegalArgumentException;

}
